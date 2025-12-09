package org.sparta.delivery.application.service;

import org.sparta.delivery.infrastructure.event.OrderApprovedEvent;
import org.sparta.delivery.presentation.dto.request.DeliveryRequest;
import org.sparta.delivery.presentation.dto.response.DeliveryResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * 배송(Delivery) 서비스 인터페이스
 *
 * 배송 생성부터 완료까지의 전체 라이프사이클 관리
 */
public interface DeliveryService {

    // ===== 생성 =====

    /**
     * 단순 생성 (테스트용)
     *
     * 컨트롤러에서 받은 값만으로 Delivery 생성
     * - 허브 경로/DeliveryLog 생성은 수행하지 않음
     * - 주문당 하나의 배송만 생성 가능 (중복 체크)
     *
     * @param request 배송 생성 요청 정보
     * @return 생성된 배송 상세 정보
     * @throws org.sparta.common.error.BusinessException DELIVERY_ALREADY_EXISTS - 이미 배송이 존재하는 경우
     */
    DeliveryResponse.Detail createSimple(DeliveryRequest.Create request);

    /**
     * 허브 경로 조회 + DeliveryLog 생성까지 포함한 정식 생성
     *
     * 멱등성 보장:
     * - eventId 기반 중복 이벤트 체크 (1차 방어)
     * - orderId 기반 중복 배송 체크 (2차 방어)
     * - 동일 이벤트 재수신 시 기존 배송 반환
     *
     * 실패 이벤트 발행 정책:
     * - 복구 불가능한 오류 (경로 없음): DeliveryFailedEvent 발행
     * - 일시적 오류 (Feign 타임아웃): 재시도 (이벤트 발행 안함)
     * - 중복 이벤트: 조용히 무시 (이벤트 발행 안함)
     *
     * 처리 순서:
     * 1. Hub FeignClient로 허브 경로 조회
     * 2. Delivery 엔티티 생성 및 저장
     * 3. 각 경로 구간(leg)별 DeliveryLog 생성
     * 4. DeliveryProcessedEvent 기록 (멱등성 보장)
     * 5. DeliveryCreatedEvent 발행
     *
     * @param event 주문 승인 이벤트
     * @return 생성된 배송 상세 정보
     * @throws org.sparta.common.error.BusinessException NO_ROUTE_AVAILABLE - 허브 경로를 찾을 수 없는 경우
     * @throws org.sparta.common.error.BusinessException CREATION_FAILED - 예기치 못한 오류로 생성 실패
     */
    DeliveryResponse.Detail createWithRoute(OrderApprovedEvent event);

    // ===== 담당자 배정 =====

    /**
     * 허브 배송 담당자 배정
     *
     * - Delivery.hubDeliveryManId 저장
     * - Delivery.status: CREATED → HUB_WAITING
     *
     * @param deliveryId 배송 ID
     * @param request 허브 배송 담당자 배정 요청
     * @return 업데이트된 배송 상세 정보
     * @throws org.sparta.common.error.BusinessException DELIVERY_NOT_FOUND - 배송을 찾을 수 없는 경우
     */
    DeliveryResponse.Detail assignHubDeliveryMan(UUID deliveryId, DeliveryRequest.AssignHubDeliveryMan request);

    /**
     * 업체 배송 담당자 배정
     *
     * - Delivery.companyDeliveryManId 저장
     * - Delivery.status: DEST_HUB_ARRIVED (유지)
     *
     * @param deliveryId 배송 ID
     * @param request 업체 배송 담당자 배정 요청
     * @return 업데이트된 배송 상세 정보
     * @throws org.sparta.common.error.BusinessException DELIVERY_NOT_FOUND - 배송을 찾을 수 없는 경우
     */
    DeliveryResponse.Detail assignCompanyDeliveryMan(UUID deliveryId, DeliveryRequest.AssignCompanyDeliveryMan request);

    // ===== 허브 구간 진행 =====

    /**
     * 허브 leg 출발 (HUB_WAITING → HUB_MOVING)
     *
     * 처리 내용:
     * - Delivery.status 변경: HUB_WAITING → HUB_MOVING
     * - Delivery.currentLogSeq 업데이트
     * - 해당 sequence의 DeliveryLog 상태 전환: HUB_WAITING → HUB_MOVING
     * - 첫 번째 허브 출발 시(sequence=0): DeliveryStartedEvent 발행
     *
     * @param deliveryId 배송 ID
     * @param request 허브 leg 출발 요청 (sequence 포함)
     * @return 업데이트된 배송 상세 정보
     * @throws org.sparta.common.error.BusinessException DELIVERY_NOT_FOUND - 배송을 찾을 수 없는 경우
     * @throws org.sparta.common.error.BusinessException INVALID_LOG_SEQUENCE - 유효하지 않은 시퀀스 번호
     */
    DeliveryResponse.Detail startHubMoving(UUID deliveryId, DeliveryRequest.StartHubMoving request);

    /**
     * 허브 leg 도착 (HUB_MOVING → HUB_WAITING 또는 DEST_HUB_ARRIVED)
     *
     * 처리 내용:
     * - Delivery.status 변경:
     *   * 중간 허브 도착: HUB_MOVING → HUB_WAITING
     *   * 마지막 허브 도착: HUB_MOVING → DEST_HUB_ARRIVED
     * - Delivery.currentLogSeq 업데이트
     * - 해당 sequence의 DeliveryLog 상태 전환: HUB_MOVING → HUB_ARRIVED
     * - DeliveryLog에 실제 거리/시간 기록
     * - 마지막 허브 도착 시: DeliveryLastHubArrivedEvent 발행 (업체 담당자 배정용)
     *
     * @param deliveryId 배송 ID
     * @param request 허브 leg 도착 요청 (sequence, actualKm, actualMinutes 포함)
     * @return 업데이트된 배송 상세 정보
     * @throws org.sparta.common.error.BusinessException DELIVERY_NOT_FOUND - 배송을 찾을 수 없는 경우
     * @throws org.sparta.common.error.BusinessException INVALID_TOTAL_LOG_SEQ - totalLogSeq가 유효하지 않은 경우
     * @throws org.sparta.common.error.BusinessException INVALID_LOG_SEQUENCE - 유효하지 않은 시퀀스 번호
     */
    DeliveryResponse.Detail completeHubMoving(UUID deliveryId, DeliveryRequest.CompleteHubMoving request);

    // ===== 업체 구간 진행 =====

    /**
     * 업체 배송 시작 (DEST_HUB_ARRIVED → COMPANY_MOVING)
     *
     * - Delivery.status 변경: DEST_HUB_ARRIVED → COMPANY_MOVING
     *
     * @param deliveryId 배송 ID
     * @return 업데이트된 배송 상세 정보
     * @throws org.sparta.common.error.BusinessException DELIVERY_NOT_FOUND - 배송을 찾을 수 없는 경우
     */
    DeliveryResponse.Detail startCompanyMoving(UUID deliveryId);

    /**
     * 배송 완료 (COMPANY_MOVING → COMPLETED)
     *
     * 처리 내용:
     * - Delivery.status 변경: COMPANY_MOVING → COMPLETED
     * - DeliveryCompletedEvent 발행
     *
     * @param deliveryId 배송 ID
     * @return 업데이트된 배송 상세 정보
     * @throws org.sparta.common.error.BusinessException DELIVERY_NOT_FOUND - 배송을 찾을 수 없는 경우
     */
    DeliveryResponse.Detail completeDelivery(UUID deliveryId);

    // ===== 취소/삭제 =====

    /**
     * 배송 취소
     *
     * - CREATED 또는 HUB_WAITING 상태에서만 취소 가능
     * - Delivery.status 변경: (현재 상태) → CANCELLED
     *
     * @param deliveryId 배송 ID
     * @return 업데이트된 배송 상세 정보
     * @throws org.sparta.common.error.BusinessException DELIVERY_NOT_FOUND - 배송을 찾을 수 없는 경우
     * @throws org.sparta.common.error.BusinessException INVALID_STATUS_FOR_CANCEL - 취소 불가능한 상태
     */
    DeliveryResponse.Detail cancel(UUID deliveryId);

    /**
     * 배송 삭제 (소프트 삭제)
     *
     * - deletedAt 타임스탬프 설정
     * - 실제 데이터는 DB에 유지
     *
     * @param deliveryId 배송 ID
     * @throws org.sparta.common.error.BusinessException DELIVERY_NOT_FOUND - 배송을 찾을 수 없는 경우
     * @throws org.sparta.common.error.BusinessException DELIVERY_ALREADY_DELETED - 이미 삭제된 배송
     */
    void delete(UUID deliveryId);

    // ===== 조회 =====

    /**
     * 배송 상세 조회
     *
     * @param deliveryId 배송 ID
     * @return 배송 상세 정보
     * @throws org.sparta.common.error.BusinessException DELIVERY_NOT_FOUND - 배송을 찾을 수 없는 경우
     */
    DeliveryResponse.Detail getDetail(UUID deliveryId);

    /**
     * 배송 검색 (페이징)
     *
     * 검색 조건:
     * - status: 배송 상태별 필터링
     * - hubId: 출발/도착 허브 ID로 필터링
     * - companyId: 출고/납품 업체 ID로 필터링
     * - sortDirection: 정렬 방향 (ASC/DESC, 기본값: ASC)
     *
     * @param request 검색 조건
     * @param pageable 페이징 정보 (page, size, sort)
     * @return 페이징된 배송 목록
     */
    DeliveryResponse.PageResult search(DeliveryRequest.Search request, Pageable pageable);
}
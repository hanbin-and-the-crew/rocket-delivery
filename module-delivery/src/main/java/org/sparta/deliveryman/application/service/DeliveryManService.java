package org.sparta.deliveryman.application.service;

import org.sparta.deliveryman.domain.entity.DeliveryMan;
import org.sparta.deliveryman.presentation.dto.request.DeliveryManRequest;
import org.sparta.deliveryman.presentation.dto.response.DeliveryManResponse;

import java.util.List;
import java.util.UUID;

public interface DeliveryManService {

    // 1. User 이벤트 기반 생성/수정/삭제

    /**
     * UserCreatedEvent 수신 시 배송 담당자 생성
     * // 근데 여기서 고려해야 될게 userRole = DELIVERY_MANAGER 인 경우에만 배송 담당자 생성
     * // 또한 userStatus가 approve 상태인 사람만 배송 담당자 생성
     * - userId, hubId, role, slackId, realName, status 기반
     * - type(HUB/COMPANY) 및 sequence 계산은 서비스 내부에서 처리
     */
    // createFromUserCreatedEvent
    DeliveryManResponse.Detail
    create(
            UUID userId,
            UUID hubId,           // HUB 타입이면 null, COMPANY 타입이면 not null
            String realName,
            String slackId,
            String userRole,        //  DELIVERY_MANAGER
            String userStatus       //  APPROVE
//            DeliveryManType type
    );

    /**
     * UserUpdateEvent 수신 시 배송 담당자 정보 동기화
     * - realName, slackId, userRole, userStatus, hubId(Company 타입)를 엔티티에 반영
     */
    // updateFromUserUpdatedEvent
    void update(
            UUID userId,
            String realName,
            String slackId,
            String userRole,
            String userStatus,
            UUID hubId
    );

    /**
     * UserDeletedEvent 수신 시 Soft Delete 처리
     */
    // deleteFromUserDeletedEvent
    void delete(UUID userId);


    // 2. 관리용 생성 (Controller에서 직접 생성하는 경우)

    /**
     * 관리자용 배송 담당자 생성
     * - DeliveryManRequest.Create 기반으로 수동 생성
     * - 내부에서 type / hubId / sequence 제약 검증
     */
    DeliveryManResponse.Detail createManually(DeliveryManRequest.Create request);


    // 3. 상태 변경 (Controller에서 직접 상태 바꾸는 유즈케이스)

    /**
     * 배송 담당자 상태 변경
     * - 허용 전이 규칙은 엔티티의 changeDeliveryManStatus에서 검증
     */
    DeliveryManResponse.Detail changeStatus(UUID deliveryManId, DeliveryManRequest.UpdateStatus newStatus);


    // 4. 배정 / 취소 (허브/업체 배송 담당자 선택 및 롤백)

    /**
     * 허브 배송 담당자 배정
     * - type=HUB, deletedAt IS NULL, sequence ASC 후보에서
     *   1) WAITING 우선 선택
     *   2) 없으면 DELIVERING 중 deliveryCount 최소 선택
     * - assignForNewDelivery() 호출 후 저장
     * - 배정된 DeliveryMan 반환 (Delivery에서 hubDeliveryManId 등에 사용)
     */
    DeliveryMan assignHubDeliveryMan();

    /**
     * 업체 배송 담당자 배정
     * - type=COMPANY, hubId 일치, deletedAt IS NULL, sequence ASC 후보에서
     *   1) WAITING 우선
     *   2) 없으면 DELIVERING 중 deliveryCount 최소 선택
     * - assignForNewDelivery() 호출 후 저장
     * - 배정된 DeliveryMan 반환 (Delivery에서 companyDeliveryManId 등에 사용)
     */
    DeliveryMan assignCompanyDeliveryMan(UUID hubId);

    /**
     * 배송 취소 등으로 배송 담당자 배정 롤백
     * - beforeStatus 필드를 사용하여:
     *   1) 배정 전 WAITING이었던 경우: DELIVERING -> WAITING
     *   2) 배정 전 DELIVERING이었던 경우: status 유지
     * - deliveryCount 1 감소 (0 미만 안 되게)
     */
    void rollbackAssignment(UUID deliveryManId);

    void unassignDelivery(UUID deliveryManId);

    // 5. 조회 / 검색

    /**
     * 배송 담당자 상세 조회
     */
    DeliveryManResponse.Detail getDetail(UUID deliveryManId);

    /**
     * 배송 담당자 검색
     * - hubId, type, status, realName 조건으로 필터링
     */
    List<DeliveryManResponse.Summary> search(DeliveryManRequest.Search request);
}

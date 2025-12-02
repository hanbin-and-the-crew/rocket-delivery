package org.sparta.delivery.application.service;

import org.sparta.delivery.presentation.dto.request.DeliveryRequest;
import org.sparta.delivery.presentation.dto.response.DeliveryResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface DeliveryService {

    // ===== 생성 =====

    /**
     * 단순 생성 (컨트롤러에서 받은 값만으로 Delivery 생성).
     * - 허브 경로/DeliveryLog 생성은 수행하지 않는다.
     */
    DeliveryResponse.Detail createSimple(DeliveryRequest.Create request);

    /**
     * 허브 경로 조회 + DeliveryLog 생성까지 포함한 정식 생성.
     * - Hub FeignClient로 경로를 조회해 각 leg별 DeliveryLog를 만든다.
     */
    DeliveryResponse.Detail createWithRoute(DeliveryRequest.Create request);

    // ===== 담당자 배정 =====

    DeliveryResponse.Detail assignHubDeliveryMan(UUID deliveryId, DeliveryRequest.AssignHubDeliveryMan request);

    DeliveryResponse.Detail assignCompanyDeliveryMan(UUID deliveryId, DeliveryRequest.AssignCompanyDeliveryMan request);

    // ===== 허브 구간 진행 =====

    /**
     * 허브 leg 출발 (HUB_WAITING → HUB_MOVING).
     * - Delivery.status / currentLogSeq 변경
     * - 해당 sequence의 DeliveryLog를 HUB_MOVING으로 전환
     */
    DeliveryResponse.Detail startHubMoving(UUID deliveryId, DeliveryRequest.StartHubMoving request);

    /**
     * 허브 leg 도착 (HUB_MOVING → HUB_WAITING 또는 DEST_HUB_ARRIVED).
     * - Delivery.status / currentLogSeq 변경
     * - Delivery.totalLogSeq를 기준으로 마지막 log 여부 판단
     * - 해당 sequence의 DeliveryLog를 HUB_ARRIVED로 전환하고 실제 거리/시간 기록
     */
    DeliveryResponse.Detail completeHubMoving(UUID deliveryId, DeliveryRequest.CompleteHubMoving request);

    // ===== 업체 구간 진행 =====

    DeliveryResponse.Detail startCompanyMoving(UUID deliveryId);

    DeliveryResponse.Detail completeDelivery(UUID deliveryId);

    // ===== 취소/삭제 =====

    DeliveryResponse.Detail cancel(UUID deliveryId);

    void delete(UUID deliveryId);

    // ===== 조회 =====

    DeliveryResponse.Detail getDetail(UUID deliveryId);

    DeliveryResponse.PageResult search(DeliveryRequest.Search request, Pageable pageable);
}

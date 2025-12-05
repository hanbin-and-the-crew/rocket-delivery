package org.sparta.deliverylog.application.service;

import org.sparta.deliverylog.presentation.dto.request.DeliveryLogRequest;
import org.sparta.deliverylog.presentation.dto.response.DeliveryLogResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface DeliveryLogService {

    // 관리자/테스트용 직접 생성
    DeliveryLogResponse.Detail create(DeliveryLogRequest.Create request);

    // 허브 담당자 배정 (CREATED -> HUB_WAITING)
    DeliveryLogResponse.Detail assignDeliveryMan(UUID logId, DeliveryLogRequest.AssignDeliveryMan request);

    // 허브 leg 출발 (HUB_WAITING -> HUB_MOVING)
    DeliveryLogResponse.Detail startLog(UUID logId);

    // 허브 leg 도착 (HUB_MOVING -> HUB_ARRIVED, actualKm/Minutes 반영)
    DeliveryLogResponse.Detail arriveLog(UUID logId, DeliveryLogRequest.Arrive request);

    // Delivery 취소에 따른 로그 취소 (CREATED/HUB_WAITING -> CANCELED)
    DeliveryLogResponse.Detail cancelFromDelivery(UUID logId);

    // 단건 조회
    DeliveryLogResponse.Detail getDetail(UUID logId);

    // 특정 Delivery에 대한 전체 로그 타임라인 (sequence 오름차순)
    List<DeliveryLogResponse.Summary> getTimelineByDeliveryId(UUID deliveryId);

    // 검색 + 페이징
    DeliveryLogResponse.PageResult search(DeliveryLogRequest.Search request, Pageable pageable);

    void delete(UUID logId);
}

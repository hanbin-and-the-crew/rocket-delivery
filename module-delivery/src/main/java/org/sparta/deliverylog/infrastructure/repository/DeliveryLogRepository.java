package org.sparta.deliverylog.infrastructure.repository;

import org.sparta.deliverylog.domain.entity.DeliveryLog;
import org.sparta.deliverylog.domain.enumeration.DeliveryRouteStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryLogRepository {

    // 생성
    DeliveryLog save(DeliveryLog deliveryLog);

    // 단건 조회
    Optional<DeliveryLog> findById(UUID deliveryLogId);

    // 배송 ID로 전체 경로 조회
    List<DeliveryLog> findByDeliveryIdOrderByHubSequence(UUID deliveryId);

    // 배송 담당자의 진행 중인 경로 조회
    List<DeliveryLog> findByDeliveryManIdAndDeliveryStatusIn(
            UUID deliveryManId,
            List<DeliveryRouteStatus> statuses
    );

    // 허브의 대기 중인 경로 조회
    List<DeliveryLog> findByDepartureHubIdAndDeliveryStatus(
            UUID hubId,
            DeliveryRouteStatus status
    );

    // 전체 목록 조회 (페이징, 삭제 제외)
    Page<DeliveryLog> findAllActive(Pageable pageable);

    // 삭제
    void delete(DeliveryLog deliveryLog);
}

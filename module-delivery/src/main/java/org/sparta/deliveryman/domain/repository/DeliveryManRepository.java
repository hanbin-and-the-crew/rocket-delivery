package org.sparta.deliveryman.domain.repository;

import org.sparta.deliveryman.domain.entity.DeliveryMan;
import org.sparta.deliveryman.domain.enumeration.DeliveryManStatus;
import org.sparta.deliveryman.domain.enumeration.DeliveryManType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryManRepository {

    DeliveryMan save(DeliveryMan deliveryMan);

    Optional<DeliveryMan> findById(UUID id);

    Optional<DeliveryMan> findByIdAndNotDeleted(UUID id);

    Page<DeliveryMan> findAllActive(Pageable pageable);

    void softDelete(DeliveryMan deliveryMan);

    List<DeliveryMan> findIdleDeliveryMenOrderByLastDeliveryCompletedAtAsc(DeliveryManStatus status);

    // 허브/업체 담당자 지정 자동화 쿼리(허브ID, 타입, 상태별, 배정 건수 오름차순)
    List<DeliveryMan> findByAffiliationHubIdAndDeliveryManTypeAndStatusOrderByAssignedDeliveryCountAsc(
            UUID affiliationHubId, DeliveryManType deliveryManType, DeliveryManStatus status
    );
}

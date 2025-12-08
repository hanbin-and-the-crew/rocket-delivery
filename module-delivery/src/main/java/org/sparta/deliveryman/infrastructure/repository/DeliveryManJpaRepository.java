package org.sparta.deliveryman.infrastructure.repository;

import org.sparta.deliveryman.domain.entity.DeliveryMan;
import org.sparta.deliveryman.domain.enumeration.DeliveryManStatus;
import org.sparta.deliveryman.domain.enumeration.DeliveryManType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryManJpaRepository extends JpaRepository<DeliveryMan, UUID> {

    @Query("select dm from DeliveryMan dm where dm.id = :id and dm.deletedAt is null")
    Optional<DeliveryMan> findByIdAndNotDeleted(@Param("id") UUID id);

    @Query("select dm from DeliveryMan dm where dm.deletedAt is null")
    Page<DeliveryMan> findAllActive(Pageable pageable);

    List<DeliveryMan> findByStatusOrderByLastDeliveryCompletedAtAsc(DeliveryManStatus status);

    // 허브 배송 담당자 자동 지정용 (허브ID/타입/상태별, 배정 건수 오름차순)
    List<DeliveryMan> findByAffiliationHubIdAndDeliveryManTypeAndStatusOrderByAssignedDeliveryCountAsc(
            UUID affiliationHubId, DeliveryManType deliveryManType, DeliveryManStatus status);

    // 업체 배송 담당자도 위 메서드로 함께 처리 가능(DeliveryManType.PARTNER로 호출)
}

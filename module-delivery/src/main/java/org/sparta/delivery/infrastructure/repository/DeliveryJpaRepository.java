package org.sparta.delivery.infrastructure.repository;

import org.sparta.delivery.application.dto.DeliverySearchCondition;
import org.sparta.delivery.domain.entity.Delivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryJpaRepository extends JpaRepository<Delivery, UUID> {

    Optional<Delivery> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Delivery> findByOrderIdAndDeletedAtIsNull(UUID orderId);

    @Query("SELECT d FROM Delivery d WHERE " +
            "d.deletedAt IS NULL " +
            "AND (:#{#condition.orderId} IS NULL OR d.orderId = :#{#condition.orderId}) " +
            "AND (:#{#condition.departureHubId} IS NULL OR d.departureHubId = :#{#condition.departureHubId}) " +
            "AND (:#{#condition.destinationHubId} IS NULL OR d.destinationHubId = :#{#condition.destinationHubId}) " +
            "AND (:#{#condition.deliveryStatus} IS NULL OR d.deliveryStatus = :#{#condition.deliveryStatus}) " +
            "AND (:#{#condition.recipientName} IS NULL OR d.recipientName LIKE %:#{#condition.recipientName}%)")
    Page<Delivery> searchDeliveries(
            @Param("condition") DeliverySearchCondition condition,
            Pageable pageable
    );
}

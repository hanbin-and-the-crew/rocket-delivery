package org.sparta.delivery.infrastructure.repository;

import org.sparta.delivery.domain.entity.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryJpaRepository extends JpaRepository<Delivery, UUID> {
    Optional<Delivery> findByIdAndDeletedAtIsNull(UUID deliveryId);
    Optional<Delivery> findByOrderIdAndDeletedAtIsNull(UUID orderId);
}

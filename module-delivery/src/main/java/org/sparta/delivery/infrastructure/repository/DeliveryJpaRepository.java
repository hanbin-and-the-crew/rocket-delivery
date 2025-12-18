package org.sparta.delivery.infrastructure.repository;

import org.sparta.delivery.domain.entity.Delivery;
import org.sparta.delivery.domain.enumeration.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryJpaRepository extends JpaRepository<Delivery, UUID> {

    Optional<Delivery> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByOrderIdAndDeletedAtIsNull(UUID orderId);

    Optional<Delivery> findByOrderIdAndDeletedAtIsNull(UUID orderId);

    List<Delivery> findByStatusNotAndCreatedAtAfterAndDeletedAtIsNull(
            DeliveryStatus status,
            LocalDateTime createdAfter);
}

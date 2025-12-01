package org.sparta.delivery.infrastructure.repository;

import org.sparta.delivery.domain.entity.Delivery;
import org.sparta.delivery.domain.enumeration.DeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryJpaRepository extends JpaRepository<Delivery, UUID> {

    Optional<Delivery> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Delivery> findByOrderIdAndDeletedAtIsNull(UUID orderId);

    boolean existsByOrderIdAndDeletedAtIsNull(UUID orderId);

    Page<Delivery> findAllByStatusAndDeletedAtIsNull(DeliveryStatus status, Pageable pageable);

    Page<Delivery> findAllByDeletedAtIsNull(Pageable pageable);

}

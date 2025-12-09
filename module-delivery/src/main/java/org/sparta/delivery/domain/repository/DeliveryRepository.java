package org.sparta.delivery.domain.repository;

import org.sparta.delivery.domain.entity.Delivery;
import org.sparta.delivery.domain.enumeration.DeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryRepository {

    Delivery save(Delivery delivery);

    Optional<Delivery> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByOrderIdAndDeletedAtIsNull(UUID orderId);

    Page<Delivery> search(
            DeliveryStatus status,
            UUID hubId,
            UUID companyId,
            Pageable pageable,
            Sort.Direction direction
    );

    Optional<Delivery> findByOrderIdAndDeletedAtIsNull(UUID orderId);
}

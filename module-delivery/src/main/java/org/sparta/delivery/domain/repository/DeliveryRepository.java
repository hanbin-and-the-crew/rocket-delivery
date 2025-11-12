package org.sparta.delivery.domain.repository;

import org.sparta.delivery.domain.entity.Delivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryRepository {
    Delivery save(Delivery delivery);
    Optional<Delivery> findById(UUID deliveryId);
    Optional<Delivery> findByIdAndDeletedAtIsNull(UUID deliveryId);
    Optional<Delivery> findByOrderIdAndDeletedAtIsNull(UUID orderId);
    Page<Delivery> findAll(Pageable pageable);
    void delete(Delivery delivery);
}

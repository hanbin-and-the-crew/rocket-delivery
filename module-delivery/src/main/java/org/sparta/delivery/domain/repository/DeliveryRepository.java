package org.sparta.delivery.domain.repository;

import org.sparta.delivery.domain.entity.Delivery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryRepository {
    Delivery save(Delivery delivery);
    Optional<Delivery> findById(UUID deliveryId);
    Optional<Delivery> findByIdAndDeletedAtIsNull(UUID deliveryId);
    Optional<Delivery> findByOrderIdAndDeletedAtIsNull(UUID orderId);
    List<Delivery> findAll();
    void delete(Delivery delivery);
}

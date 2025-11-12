package org.sparta.delivery.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.sparta.delivery.domain.entity.Delivery;
import org.sparta.delivery.domain.repository.DeliveryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DeliveryRepositoryImpl implements DeliveryRepository {

    private final DeliveryJpaRepository deliveryJpaRepository;

    @Override
    public Delivery save(Delivery delivery) {
        return deliveryJpaRepository.save(delivery);
    }

    @Override
    public Optional<Delivery> findById(UUID deliveryId) {
        return deliveryJpaRepository.findById(deliveryId);
    }

    @Override
    public Optional<Delivery> findByIdAndDeletedAtIsNull(UUID deliveryId) {
        return deliveryJpaRepository.findByIdAndDeletedAtIsNull(deliveryId);
    }

    @Override
    public Optional<Delivery> findByOrderIdAndDeletedAtIsNull(UUID orderId) {
        return deliveryJpaRepository.findByOrderIdAndDeletedAtIsNull(orderId);
    }

    @Override
    public List<Delivery> findAll() {
        return deliveryJpaRepository.findAll();
    }

    @Override
    public void delete(Delivery delivery) {
        deliveryJpaRepository.delete(delivery);
    }
}

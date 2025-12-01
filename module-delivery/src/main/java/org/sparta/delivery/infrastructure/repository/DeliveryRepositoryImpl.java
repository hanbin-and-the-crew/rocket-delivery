package org.sparta.delivery.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.sparta.delivery.domain.entity.Delivery;
import org.sparta.delivery.domain.enumeration.DeliveryStatus;
import org.sparta.delivery.domain.repository.DeliveryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

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
    public Optional<Delivery> findByIdAndDeletedAtIsNull(UUID id) {
        return deliveryJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public Optional<Delivery> findByOrderIdAndDeletedAtIsNull(UUID orderId) {
        return deliveryJpaRepository.findByOrderIdAndDeletedAtIsNull(orderId);
    }

    @Override
    public boolean existsByOrderIdAndDeletedAtIsNull(UUID orderId) {
        return deliveryJpaRepository.existsByOrderIdAndDeletedAtIsNull(orderId);
    }

    @Override
    public Page<Delivery> findAllByStatusAndDeletedAtIsNull(DeliveryStatus status, Pageable pageable) {
        return deliveryJpaRepository.findAllByStatusAndDeletedAtIsNull(status, pageable);
    }

    @Override
    public Page<Delivery> findAllByDeletedAtIsNull(Pageable pageable) {
        return deliveryJpaRepository.findAllByDeletedAtIsNull(pageable);
    }

}

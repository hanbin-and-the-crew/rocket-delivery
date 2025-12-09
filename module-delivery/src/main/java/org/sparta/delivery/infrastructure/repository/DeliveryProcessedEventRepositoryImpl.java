package org.sparta.delivery.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.sparta.delivery.domain.entity.DeliveryProcessedEvent;
import org.sparta.delivery.domain.repository.DeliveryProcessedEventRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DeliveryProcessedEventRepositoryImpl implements DeliveryProcessedEventRepository {

    private final DeliveryProcessedEventJpaRepository jpaRepository;

    @Override
    public boolean existsByEventId(UUID eventId) {
        return jpaRepository.existsByEventId(eventId);
    }

    @Override
    public DeliveryProcessedEvent save(DeliveryProcessedEvent deliveryProcessedEvent) {
        return jpaRepository.save(deliveryProcessedEvent);
    }
}
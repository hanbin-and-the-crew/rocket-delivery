package org.sparta.delivery.infrastructure.repository;

import org.sparta.delivery.domain.entity.DeliveryProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * DeliveryProcessedEvent JPA Repository
 */
public interface DeliveryProcessedEventJpaRepository extends JpaRepository<DeliveryProcessedEvent, UUID> {

    boolean existsByEventId(UUID eventId);
}
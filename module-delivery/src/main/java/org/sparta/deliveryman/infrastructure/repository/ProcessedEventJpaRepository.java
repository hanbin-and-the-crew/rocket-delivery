package org.sparta.deliveryman.infrastructure.repository;

import org.sparta.deliveryman.domain.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * DeliveryProcessedEvent JPA Repository
 */
public interface ProcessedEventJpaRepository extends JpaRepository<ProcessedEvent, UUID> {

    boolean existsByEventId(UUID eventId);

    Optional<ProcessedEvent> findByEventId(UUID eventId);

    long countByEventId(UUID eventId);
}
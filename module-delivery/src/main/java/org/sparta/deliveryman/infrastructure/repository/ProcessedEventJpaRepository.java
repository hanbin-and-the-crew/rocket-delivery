package org.sparta.deliveryman.infrastructure.repository;

import org.sparta.deliveryman.domain.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * ProcessedEvent JPA Repository
 */
public interface ProcessedEventJpaRepository extends JpaRepository<ProcessedEvent, UUID> {

    boolean existsByEventId(UUID eventId);
}
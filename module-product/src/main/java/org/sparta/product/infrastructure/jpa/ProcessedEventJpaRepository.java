package org.sparta.product.infrastructure.jpa;

import org.sparta.product.domain.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * JPA 기반 Product 이벤트 처리 이력 저장소
 */
public interface ProcessedEventJpaRepository extends JpaRepository<ProcessedEvent, UUID> {

    boolean existsByEventId(UUID eventId);
}

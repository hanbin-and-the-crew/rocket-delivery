package org.sparta.delivery.infrastructure.repository;

import org.sparta.delivery.domain.entity.DeliveryOutboxEvent;
import org.sparta.delivery.domain.enumeration.OutboxStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DeliveryOutboxEventJpaRepository extends JpaRepository<DeliveryOutboxEvent, UUID> {

    Page<DeliveryOutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxStatus status, Pageable pageable);
}
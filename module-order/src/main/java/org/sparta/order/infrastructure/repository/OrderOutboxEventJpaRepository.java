package org.sparta.order.infrastructure.repository;

import org.sparta.order.domain.entity.OrderOutboxEvent;
import org.sparta.order.domain.enumeration.OutboxStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderOutboxEventJpaRepository extends JpaRepository<OrderOutboxEvent, UUID> {

    Page<OrderOutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxStatus status, Pageable pageable);
}
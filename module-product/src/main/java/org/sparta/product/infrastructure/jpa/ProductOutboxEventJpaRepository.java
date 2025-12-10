package org.sparta.product.infrastructure.jpa;

import org.sparta.product.domain.outbox.OutboxStatus;
import org.sparta.product.domain.outbox.ProductOutboxEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductOutboxEventJpaRepository extends JpaRepository<ProductOutboxEvent, UUID> {

    Page<ProductOutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxStatus status, Pageable pageable);

    long countByStatus(OutboxStatus status);

}

package org.sparta.payment.infrastructure.repository;

import org.sparta.payment.domain.entity.PaymentOutbox;
import org.sparta.common.domain.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentOutboxJpaRepository extends JpaRepository<PaymentOutbox, UUID> {

    List<PaymentOutbox> findByStatus(OutboxStatus status);

    List<PaymentOutbox> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus status);
}

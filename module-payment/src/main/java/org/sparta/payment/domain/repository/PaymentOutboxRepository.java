package org.sparta.payment.domain.repository;

import org.sparta.payment.domain.entity.PaymentOutbox;
import org.sparta.common.domain.OutboxStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentOutboxRepository {

    PaymentOutbox save(PaymentOutbox outbox);

    Optional<PaymentOutbox> findById(UUID id);

    List<PaymentOutbox> findByStatus(OutboxStatus status);

    List<PaymentOutbox> findTop100ByStatus(OutboxStatus status);

    List<PaymentOutbox> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus status);


}

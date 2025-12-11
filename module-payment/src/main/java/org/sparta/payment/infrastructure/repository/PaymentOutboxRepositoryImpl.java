package org.sparta.payment.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.sparta.payment.domain.entity.PaymentOutbox;
import org.sparta.common.domain.OutboxStatus;
import org.sparta.payment.domain.repository.PaymentOutboxRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PaymentOutboxRepositoryImpl implements PaymentOutboxRepository {

    private final PaymentOutboxJpaRepository jpa;

    @Override
    public PaymentOutbox save(PaymentOutbox outbox) {
        return jpa.save(outbox);
    }

    @Override
    public Optional<PaymentOutbox> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public List<PaymentOutbox> findByStatus(OutboxStatus status) {
        return jpa.findByStatus(status);
    }

    @Override
    public List<PaymentOutbox> findTop100ByStatus(OutboxStatus status) {
        return jpa.findTop100ByStatusOrderByCreatedAtAsc(status);
    }

    @Override
    public List<PaymentOutbox> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus status) {
        return jpa.findTop100ByStatusOrderByCreatedAtAsc(status);
    }
}

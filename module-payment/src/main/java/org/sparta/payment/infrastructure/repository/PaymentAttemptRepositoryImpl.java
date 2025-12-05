package org.sparta.payment.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.sparta.payment.domain.entity.PaymentAttempt;
import org.sparta.payment.domain.repository.PaymentAttemptRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PaymentAttemptRepositoryImpl implements PaymentAttemptRepository {

    private final PaymentAttemptJpaRepository jpa;

    @Override
    public PaymentAttempt save(PaymentAttempt attempt) {
        return jpa.save(attempt);
    }

    @Override
    public Optional<PaymentAttempt> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public List<PaymentAttempt> findByPaymentId(UUID paymentId) {
        return jpa.findByPaymentId(paymentId);
    }
}

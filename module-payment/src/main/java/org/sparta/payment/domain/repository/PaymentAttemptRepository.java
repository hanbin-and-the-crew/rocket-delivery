package org.sparta.payment.domain.repository;

import org.sparta.payment.domain.entity.PaymentAttempt;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentAttemptRepository {

    PaymentAttempt save(PaymentAttempt attempt);

    Optional<PaymentAttempt> findById(UUID id);

    List<PaymentAttempt> findByPaymentId(UUID paymentId);
}

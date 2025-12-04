package org.sparta.payment.infrastructure.repository;


import org.sparta.payment.domain.entity.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentAttemptJpaRepository extends JpaRepository<PaymentAttempt, UUID> {

    List<PaymentAttempt> findByPaymentId(UUID paymentId);
}

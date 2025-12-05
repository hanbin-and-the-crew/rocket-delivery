package org.sparta.payment.infrastructure.repository;

import org.sparta.payment.domain.entity.Payment;
import org.sparta.payment.domain.enumeration.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentJpaRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByOrderId(UUID orderId);

    List<Payment> findAllByStatus(PaymentStatus status);

    Optional<Payment> findByPaymentKey(String paymentKey);
}

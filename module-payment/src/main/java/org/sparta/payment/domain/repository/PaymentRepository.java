package org.sparta.payment.domain.repository;


import org.sparta.payment.domain.entity.Payment;
import org.sparta.payment.domain.enumeration.PaymentStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(UUID paymentId);

    Optional<Payment> findByOrderId(UUID orderId);

    List<Payment> findAllByStatus(PaymentStatus status);

    void delete(Payment payment);
}

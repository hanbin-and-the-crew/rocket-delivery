package org.sparta.order.domain.repository;

import org.sparta.order.domain.entity.Payment;

import java.util.List;
import java.util.UUID;

public interface PaymentRepository {

    Payment save(Payment payment);

    List<Payment> findByOrderId(UUID orderId);
}
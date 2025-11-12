package org.sparta.order.domain.repository;


import org.sparta.order.domain.entity.Payment;

public interface PaymentRepository {

    Payment save(Payment payment);

}
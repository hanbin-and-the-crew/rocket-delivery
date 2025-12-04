package org.sparta.payment.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.sparta.payment.domain.entity.Payment;
import org.sparta.payment.domain.enumeration.PaymentStatus;
import org.sparta.payment.domain.repository.PaymentRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {

    private final PaymentJpaRepository jpa;

    @Override
    public Payment save(Payment payment) {
        return jpa.save(payment);
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<Payment> findByOrderId(UUID orderId) {
        return jpa.findByOrderId(orderId);
    }

    @Override
    public List<Payment> findAllByStatus(PaymentStatus status) {
        return jpa.findAllByStatus(status);
    }

    @Override
    public void delete(Payment payment) {
        jpa.delete(payment);
    }
}

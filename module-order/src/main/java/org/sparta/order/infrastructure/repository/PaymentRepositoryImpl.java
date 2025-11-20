package org.sparta.order.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.sparta.order.domain.entity.Payment;
import org.sparta.order.domain.repository.PaymentRepository;

/**
 * Payment Repository 구현체
 * QueryDSL을 활용한 동적 쿼리 처리
 */

@RequiredArgsConstructor
public abstract class PaymentRepositoryImpl implements PaymentRepository {

    private final PaymentJpaRepository paymentJpaRepository;

    @Override
    public Payment save(Payment order) {
        return paymentJpaRepository.save(order);
    }

}
package org.sparta.order.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.order.application.dto.request.PaymentRequest;
import org.sparta.order.domain.entity.Payment;
import org.sparta.order.infrastructure.repository.PaymentJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;


/**
 * 3주차 과제용 간이 PaymentService
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {
    private final PaymentJpaRepository paymentRepository;

    @Transactional
    public void processPayment(PaymentRequest.Create request, UUID userId) {
        Payment payment = Payment.create("payment이름");

        Payment savedPayment = paymentRepository.save(payment);
        log.info("결제 생성 완료 - orderId: {}", savedPayment.getId());
        return;
    }
}

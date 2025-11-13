package org.sparta.order.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.event.EventPublisher;
import org.sparta.order.application.dto.request.PaymentRequest;
import org.sparta.order.domain.entity.Payment;
import org.sparta.order.infrastructure.event.publisher.PaymentCompletedSpringEvent;
import org.sparta.order.infrastructure.repository.PaymentJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final EventPublisher eventPublisher; // Spring 이벤트 퍼블리셔

    @Transactional
    public void processPayment(PaymentRequest.Create request, UUID userId) {

        // 간단한 결제 처리 로직
        // product로부터 가져와서 계산하는 Logic은 다음에 작성!!!!!!!!!!!!!!!
        BigDecimal totalAmount = new BigDecimal("5000.00"); // calculateAmount(request.productId(), request.quantity());
        Payment payment = Payment.create(request.orderId(), request.productId(), request.quantity(), totalAmount);

        if (payment.isValid()) {
            payment.complete(); // 실무에서는 PG사 연동 등의 복잡한 로직이 들어갑니다

            Payment savedPayment = paymentRepository.save(payment);

            // 결제 완료 이벤트 발행 
            // Spring Event랑 Kafka 둘 다
            eventPublisher.publishLocal(PaymentCompletedSpringEvent.of(savedPayment, userId));
            //eventPublisher.publishExternal(PaymentCompletedEvent.of(savedPayment));

            log.info("결제 생성 완료 - paymentId: {}", savedPayment.getId());
        } else {
            payment.fail(); // 실무에서는 PG사 연동 등의 복잡한 로직이 들어갑니다

            Payment savedPayment = paymentRepository.save(payment);

            log.info("결제 생성 실패 - paymentId: {}", savedPayment.getId());
        }
        return;
    }
}
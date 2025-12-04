package org.sparta.user.application.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.user.application.command.PointCommand;
import org.sparta.user.application.service.PointService;
import org.sparta.user.infrastructure.event.PaymentEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User 모듈 Point 결제 관련 이벤트 핸들러
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointEventHandler {

    private final PointService pointService;

    @KafkaListener(topics = "payment-success", groupId = "user-service")
    @Transactional
    public void handleOrderCreated(PaymentEvent event) {
        log.info("결제 성공 이벤트 수신: orderId={}", event.orderId());

        PointCommand.ConfirmPoint command = new PointCommand.ConfirmPoint(event.orderId());
        pointService.confirmPointUsage(command);
    }

    @KafkaListener(topics = "payment-failed", groupId = "product-service")
    @Transactional
    public void handlePaymentCompleted(PaymentEvent event) {
        log.info("결제 실패 이벤트 수신: orderId={}", event.orderId());

        pointService.rollbackReservations(event.orderId());
    }
}
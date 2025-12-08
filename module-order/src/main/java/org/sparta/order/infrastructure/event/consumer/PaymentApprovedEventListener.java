package org.sparta.order.infrastructure.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.order.application.service.OrderService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * [ PaymentApprovedEvent 수신 ]
 * Payment 모듈에서 결제 승인 이벤트를 받아서 Order 승인 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentApprovedEventListener {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "payment-events",  // Payment 모듈의 토픽
            groupId = "order-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentApproved(String message) {
        try {
            PaymentApprovedEvent event = objectMapper.readValue(message, PaymentApprovedEvent.class);
            log.info("Received PaymentApprovedEvent: paymentId={}, orderId={}, eventId={}",
                    event.paymentId(), event.orderId(), event.eventId());

            // Order 승인 처리 (PENDING → APPROVED) + OrderApprovedEvent 발행
            orderService.approveOrder(event.orderId(), event.paymentId());

            log.info("Order approved successfully: orderId={}, paymentId={}",
                    event.orderId(), event.paymentId());

        } catch (Exception e) {
            log.error("Failed to handle payment approved event: {}", message, e);
            throw new RuntimeException("Order approval failed", e);
        }
    }
}

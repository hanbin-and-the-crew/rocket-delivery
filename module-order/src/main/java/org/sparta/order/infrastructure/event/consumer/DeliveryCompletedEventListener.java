package org.sparta.order.infrastructure.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.order.application.service.OrderService;
import org.sparta.order.presentation.dto.response.OrderResponse;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * [ DeliveryCompletedEvent 수신 ]
 * delivery 모듈에서 최종 배송 완료 이벤트를 받아서 Order 배송 완료 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryCompletedEventListener {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "delivery-events",  // Delivery 모듈의 토픽
            groupId = "order-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleDeliveryCompleted(String message) {
        try {
            DeliveryCompletedEvent event = objectMapper.readValue(message, DeliveryCompletedEvent.class);
            log.info("Received DeliveryCompletedEvent: deliveryId={}, orderId={}, eventId={}",
                    event.deliveryId(), event.orderId(), event.eventId());

            // Order 배송 완료 처리 (SHIPPING -> DELIVERED)
            OrderResponse.Update result = orderService.deliveredOrder(event.orderId());

            log.info("Order delivered: orderId={}, message={}",
                    event.orderId(),
                    result.message()  // 응답 메시지
            );

        } catch (Exception e) {
            log.error("Failed to handle delivery completed event: {}", message, e);
            throw new RuntimeException("Order delivered failed", e);
        }
    }
}
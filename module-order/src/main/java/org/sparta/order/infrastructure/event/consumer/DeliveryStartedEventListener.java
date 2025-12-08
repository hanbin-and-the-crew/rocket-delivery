package org.sparta.order.infrastructure.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.order.application.service.OrderService;
import org.sparta.order.presentation.dto.response.OrderResponse;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * [ DelieryStartedEvent 수신 ]
 * delivery 모듈에서 배송 출발 이벤트를 받아서 Order 출고 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryStartedEventListener {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "delivery-events",  // Delivery 모듈의 토픽
            groupId = "order-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleDeliveryStarted(String message) {
        try {
            DeliveryStartedEvent event = objectMapper.readValue(message, DeliveryStartedEvent.class);
            log.info("Received DeliveryStartedEvent: deliveryId={}, orderId={}, eventId={}",
                    event.deliveryId(), event.orderId(), event.eventId());

            // Order 출고 처리 (APPROVED → SHIPPING)
            OrderResponse.Update result = orderService.shippedOrder(event.orderId());

            log.info("Order shipped: orderId={}, message={}",
                    event.orderId(),
                    result.message()  // 응답 메시지
            );

        } catch (Exception e) {
            log.error("Failed to handle delivery started event: {}", message, e);
            throw new RuntimeException("Order shipping failed", e);
        }
    }
}
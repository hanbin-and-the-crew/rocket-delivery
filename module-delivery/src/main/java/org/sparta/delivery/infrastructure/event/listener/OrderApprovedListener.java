package org.sparta.delivery.infrastructure.event.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.delivery.application.service.DeliveryServiceImpl;
import org.sparta.delivery.infrastructure.event.OrderApprovedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderApprovedListener {

    private final DeliveryServiceImpl deliveryService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "order-events",
            groupId = "delivery-events"
    )
    public void handleOrderApproved(String message) {
        try {
            OrderApprovedEvent event = objectMapper.readValue(message, OrderApprovedEvent.class);
            log.info("OrderApprovedEvent received: orderId={}", event.orderId());

            deliveryService.createWithRoute(event);

        } catch (Exception e) {
            log.error("Failed to handle order approved event", e);
            throw new RuntimeException(e);
        }
    }
}

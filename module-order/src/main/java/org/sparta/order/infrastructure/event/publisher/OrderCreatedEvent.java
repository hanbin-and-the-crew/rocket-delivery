package org.sparta.order.infrastructure.event.publisher;

import org.sparta.common.event.DomainEvent;
import org.sparta.order.domain.entity.Order;

import java.time.Instant;
import java.util.UUID;

/**
 * 주문 생성 Kafka 이벤트
 */
public record OrderCreatedEvent(
        UUID eventId,
        UUID orderId,
        UUID productId,
        int quantity,
        UUID userId,
        Instant occurredAt
) implements DomainEvent {
    public static OrderCreatedEvent of(Order order, UUID userId) {
        return new OrderCreatedEvent(
                UUID.randomUUID(),              // eventId (멱등성 보장용)
                order.getId(),
                order.getProductId(),
                order.getQuantity().getValue(),
                userId,
                java.time.Instant.now()
        );
    }
}
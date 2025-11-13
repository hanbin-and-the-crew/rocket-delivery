package org.sparta.order.infrastructure.event.publisher;

import org.sparta.common.event.DomainEvent;
import org.sparta.order.domain.entity.Order;

import java.time.Instant;
import java.util.UUID;

/**
 * 주문 취소 Kafka 이벤트
 */
public record OrderCanceledEvent(
        UUID eventId,
        UUID orderId,
        UUID productId,
        int quantity,
        Instant occurredAt
) implements DomainEvent {
    public static OrderCanceledEvent of(Order order) {
        return new OrderCanceledEvent(
                UUID.randomUUID(),              // eventId (멱등성 보장용)
                order.getId(),
                order.getProductId(),
                order.getQuantity().getValue(),
                Instant.now()
        );
    }
}
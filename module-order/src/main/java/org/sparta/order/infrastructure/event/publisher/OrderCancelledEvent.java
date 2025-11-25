package org.sparta.order.infrastructure.event.publisher;

import org.sparta.common.event.DomainEvent;
import org.sparta.order.domain.entity.Order;

import java.time.Instant;
import java.util.UUID;

/**
 * order event 발행
 * 주문 취소 Kafka 이벤트
 */
public record OrderCancelledEvent(
        UUID eventId,
        UUID orderId,
        UUID productId,
        int quantity,
        Instant occurredAt
) implements DomainEvent {
    public static OrderCancelledEvent of(Order order) {
        return new OrderCancelledEvent(
                UUID.randomUUID(),              // eventId (멱등성 보장용)
                order.getId(),
                order.getProductId(),
                order.getQuantity().getValue(),
                Instant.now()
        );
    }
}
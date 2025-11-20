package org.sparta.order.infrastructure.event.publisher;

import org.sparta.common.event.DomainEvent;
import org.sparta.order.domain.entity.Order;

import java.time.Instant;
import java.util.UUID;

/**
 * 주문 생성 Spring Event
 */
public record OrderCreatedSpringEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        UUID productId,
        int quantity,
        UUID userId
) implements DomainEvent {
    public static OrderCreatedSpringEvent of(Order order, UUID userId) {
        return new OrderCreatedSpringEvent(
                UUID.randomUUID(),              // eventId (멱등성 보장용)
                Instant.now(),
                order.getId(),
                order.getProductId(),
                order.getQuantity().getValue(),
                userId // Order Entity에는 userId가 없으므로 따로 받아준다.
        );
    }
}
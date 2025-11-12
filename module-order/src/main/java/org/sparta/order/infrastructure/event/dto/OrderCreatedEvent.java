package org.sparta.order.infrastructure.event.dto;

import org.sparta.common.event.DomainEvent;
import org.sparta.order.domain.entity.Order;

import java.time.Instant;
import java.util.UUID;

/**
 * 주문 생성 Spring Event
 */
public record OrderCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        UUID productId,
        int quantity,
        UUID userId
) implements DomainEvent {
    public static OrderCreatedEvent of(Order order, UUID userId) {
        return new OrderCreatedEvent(
                UUID.randomUUID(),              // eventId (멱등성 보장용)
                Instant.now(),
                order.getId(),
                order.getProductId(),
                order.getQuantity().getValue(),
                userId // Order Entity에는 userId가 없으므로 따로 받아준다.
        );
    }
}
package org.sparta.order.infrastructure.event.publisher;

import org.sparta.common.event.DomainEvent;
import org.sparta.order.domain.entity.Order;

import java.time.Instant;
import java.util.UUID;

/**
 * 결제 생성 Spring Event
 */
public record PaymentCompletedEvent(
        UUID eventId,
        UUID orderId,
        UUID productId,
        int quantity,
        Instant occurredAt
) implements DomainEvent {
    public static PaymentCompletedEvent of(Order order) {
        return new PaymentCompletedEvent(
                java.util.UUID.randomUUID(),              // eventId (멱등성 보장용)
                order.getId(),
                order.getProductId(),
                order.getQuantity().getValue(),
                Instant.now()
        );
    }
}
package org.sparta.order.infrastructure.event.publisher;

import org.sparta.common.event.DomainEvent;
import org.sparta.order.domain.entity.Order;

import java.time.Instant;
import java.util.UUID;

public record OrderApprovedEvent(
        UUID eventId,
        UUID orderId,
        UUID userId,
        Instant occurredAt
) implements DomainEvent {
    public static OrderApprovedEvent of(Order order) {
        return new OrderApprovedEvent(
                UUID.randomUUID(),              // eventId (멱등성 보장용)
                order.getId(),
                order.getCustomerId(),
                Instant.now()
        );
    }
}

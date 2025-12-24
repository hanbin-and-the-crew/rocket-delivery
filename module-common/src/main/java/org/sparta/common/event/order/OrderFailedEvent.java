package org.sparta.common.event.order;

import org.sparta.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record OrderFailedEvent(
        UUID orderId,
        String errorReason,
        UUID eventId,
        Instant occurredAt
) implements DomainEvent {
    public static OrderFailedEvent of(
            UUID orderId,
            String errorReason
    ) {
        return new OrderFailedEvent(
                orderId,
                errorReason,
                UUID.randomUUID(),
                Instant.now()
        );
    }
}


package org.sparta.common.event.delivery;

import org.sparta.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record DeliveryCreatedEvent(
        UUID deliveryId,
        UUID orderId,
        UUID eventId,
        Instant occurredAt
) implements DomainEvent {
    public static DeliveryCreatedEvent of(
            UUID deliveryId,
            UUID orderId
    ) {
        return new DeliveryCreatedEvent(
                deliveryId,
                orderId,
                UUID.randomUUID(),
                Instant.now()
        );
    }
}

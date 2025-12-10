package org.sparta.deliveryman.infrastructure.event;

import org.sparta.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record UserDeletedEvent(
        UUID eventId,
        UUID userId,
        Instant occurredAt
) implements DomainEvent {
    public static UserDeletedEvent of(UUID userId) {
        return new UserDeletedEvent(
                UUID.randomUUID(),
                userId,
                Instant.now()
        );
    }
}

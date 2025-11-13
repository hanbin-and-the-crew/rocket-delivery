package org.sparta.user.infrastructure.event.publisher;

import org.sparta.common.event.DomainEvent;
import org.sparta.user.domain.entity.User;

import java.time.Instant;
import java.util.UUID;

public record UserCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        UserPayload payload
) implements DomainEvent {
    public static UserCreatedEvent of(User user) {
        return new UserCreatedEvent(
                UUID.randomUUID(),
                Instant.now(),
                UserPayload.from(user)
        );
    }
}
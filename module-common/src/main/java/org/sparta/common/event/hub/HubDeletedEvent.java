package org.sparta.common.event.hub;

import org.sparta.common.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record HubDeletedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID hubId
) implements DomainEvent {
    public static HubDeletedEvent of(UUID hubId) {
        return new HubDeletedEvent(UUID.randomUUID(), Instant.now(), hubId);
    }
}

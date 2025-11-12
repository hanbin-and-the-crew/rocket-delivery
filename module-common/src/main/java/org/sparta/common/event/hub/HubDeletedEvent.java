package org.sparta.common.event.hub;

import org.sparta.common.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record HubDeletedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID hubId
) implements DomainEvent {
    public HubDeletedEvent(UUID hubId) {
        this(UUID.randomUUID(), Instant.now(), hubId);
    }
}

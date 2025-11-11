package org.sparta.hub.domain.event;

import org.sparta.common.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record HubCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID hubId,
        String name,
        String region
) implements DomainEvent {
    public HubCreatedEvent(UUID hubId, String name, String region) {
        this(UUID.randomUUID(), Instant.now(), hubId, name, region);
    }
}


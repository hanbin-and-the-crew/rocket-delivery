package org.sparta.common.event.hub;

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

    public static HubCreatedEvent of(UUID hubId, String name, String address) {
        return new HubCreatedEvent(UUID.randomUUID(), Instant.now(), hubId, name, address);
    }
}


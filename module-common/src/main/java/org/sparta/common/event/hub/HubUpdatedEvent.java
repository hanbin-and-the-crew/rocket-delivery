package org.sparta.common.event.hub;

import org.sparta.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record HubUpdatedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID hubId,
        String name,
        String address
) implements DomainEvent {
    public static HubUpdatedEvent of(UUID hubId, String name, String address) {
        return new HubUpdatedEvent(UUID.randomUUID(), Instant.now(), hubId, name, address);
    }
}

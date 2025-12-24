package org.sparta.common.event.payment;

import org.sparta.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record GenericDomainEvent(
        UUID eventId,
        Instant occurredAt,
        String eventType,
        Object payload
) implements DomainEvent {

    public GenericDomainEvent(String eventType, Object payload) {
        this(UUID.randomUUID(), Instant.now(), eventType, payload);
    }
}

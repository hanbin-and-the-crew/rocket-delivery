package org.sparta.common.event;

import java.time.Instant;
import java.util.UUID;

public record EventEnvelope<T>(
        UUID eventId,
        String eventType,
        String aggregateType,
        String aggregateId,
        Instant occurredAt,
        String source,
        int version,
        T payload
) {
    public static <T> EventEnvelope<T> of(
            String eventType,
            String aggregateType,
            String aggregateId,
            String source,
            int version,
            T payload
    ) {
        return new EventEnvelope<>(
                UUID.randomUUID(),
                eventType,
                aggregateType,
                aggregateId,
                Instant.now(),
                source,
                version,
                payload
        );
    }
}

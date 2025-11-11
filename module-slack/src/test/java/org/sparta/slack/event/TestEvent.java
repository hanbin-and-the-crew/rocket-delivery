package org.sparta.slack.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.sparta.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public class TestEvent implements DomainEvent {

    private final UUID eventId;
    private final Instant occurredAt;
    private final String message;

    @JsonCreator
    public TestEvent(@JsonProperty("message") String message,
                     @JsonProperty("eventId") UUID eventId,
                     @JsonProperty("occurredAt") Instant occurredAt) {
        this.eventId = eventId != null ? eventId : UUID.randomUUID();
        this.occurredAt = occurredAt != null ? occurredAt : Instant.now();
        this.message = message;
    }

    // 기존 생성자 유지(옵션)
    public TestEvent(String message) {
        this(UUID.randomUUID(), Instant.now(), message);
    }

    public TestEvent(UUID eventId, Instant occurredAt, String message) {
        this.eventId = eventId;
        this.occurredAt = occurredAt;
        this.message = message;
    }

    @Override public UUID eventId() { return eventId; }
    @Override public Instant occurredAt() { return occurredAt; }
    public String getMessage() { return message; }

    @Override
    public String toString() {
        return "TestEvent{" +
                "eventId=" + eventId +
                ", message='" + message + '\'' +
                ", occurredAt=" + occurredAt +
                '}';
    }
}

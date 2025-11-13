package org.sparta.common.event.slack;

import org.sparta.common.event.DomainEvent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * user-events 토픽 이벤트 DTO
 */
public record UserDomainEvent(
        UUID eventId,
        Instant occurredAt,
        String eventType,
        Payload payload
) implements DomainEvent {

    public record Payload(
            UUID userId,
            String userName,
            String realName,
            String slackId,
            String role,
            String status,
            UUID hubId
    ) {
    }

    public boolean hasPayload() {
        return payload != null && payload.userId() != null;
    }

    public LocalDateTime eventTime() {
        return LocalDateTime.ofInstant(occurredAt, ZoneId.of("Asia/Seoul"));
    }
}

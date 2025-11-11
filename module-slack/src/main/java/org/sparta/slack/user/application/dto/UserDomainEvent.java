package org.sparta.slack.user.application.dto;

import org.sparta.common.event.DomainEvent;
import org.sparta.slack.user.domain.enums.UserRole;
import org.sparta.slack.user.domain.enums.UserStatus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Kafka user-events 토픽 역직렬화용 이벤트 DTO
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
            UserRole role,
            UserStatus status,
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

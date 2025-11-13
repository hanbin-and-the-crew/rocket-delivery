package org.sparta.common.event.slack;

import org.sparta.common.event.DomainEvent;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;

/**
 * 주문 서비스가 발송 시한 Slack 알림을 요청할 때 사용하는 도메인 이벤트.
 * 주문 모듈에서도 동일 레코드를 사용하거나, 동일한 필드를 가진 DTO를 발행해야 합니다.
 */
public record OrderDeadlineRequestedEvent(
        UUID eventId,
        Instant occurredAt,
        Payload payload
) implements DomainEvent {

    public OrderDeadlineRequestedEvent {
        if (eventId == null) {
            throw new IllegalArgumentException("eventId는 필수입니다");
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
        if (payload == null) {
            throw new IllegalArgumentException("payload는 필수입니다");
        }
    }

    public record Payload(
            UUID orderId,
            String orderNumber,
            String customerName,
            String customerEmail,
            LocalDateTime orderTime,
            String productInfo,
            Integer quantity,
            String requestMemo,
            UUID originHubId,
            String originHubName,
            String originAddress,
            String originLabel,
            UUID destinationCompanyId,
            String destinationCompanyName,
            String destinationAddress,
            String destinationLabel,
            String transitPath,
            LocalDateTime deliveryDeadline,
            Integer workStartHour,
            Integer workEndHour,
            Set<String> targetRoles,
            String deliveryManagerName,
            String deliveryManagerEmail
    ) {
    }

    public LocalDateTime occurredAtSeoul() {
        return LocalDateTime.ofInstant(occurredAt, ZoneId.of("Asia/Seoul"));
    }
}

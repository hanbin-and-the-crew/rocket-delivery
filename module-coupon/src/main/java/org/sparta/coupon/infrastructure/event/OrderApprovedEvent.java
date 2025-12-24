package org.sparta.coupon.infrastructure.event;

import java.time.Instant;
import java.util.UUID;

public record OrderApprovedEvent(
        UUID eventId,       // 멱등성 보장용
        UUID orderId,
        UUID userId,
        Instant occurredAt
) {
}
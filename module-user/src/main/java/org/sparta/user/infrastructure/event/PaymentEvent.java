package org.sparta.user.infrastructure.event;

import java.time.Instant;
import java.util.UUID;

/**
 * 결제 생성 이벤트, 수신용 이벤트 DTO
 *
 */
public record PaymentEvent (
        UUID eventId,       // 멱등성 보장용
        UUID orderId,
        Instant occurredAt
) {
}
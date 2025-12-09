package org.sparta.user.infrastructure.event.publisher;

import org.sparta.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Order 모듈로 전달:
 * - 예약된 포인트가 실제로 사용 확정되었음을 알림
 * - Order 모듈에서 포인트 할인이 최종 확정되었음을 인지
 */
public record PointConfirmedEvent(
        UUID eventId,
        UUID orderId,
        Long discountAmount,
        Instant occurredAt
) implements DomainEvent {

    public static PointConfirmedEvent of(UUID orderId, Long discountAmount) {
        return new PointConfirmedEvent(
                UUID.randomUUID(),
                orderId,
                discountAmount,
                Instant.now()
        );
    }
}
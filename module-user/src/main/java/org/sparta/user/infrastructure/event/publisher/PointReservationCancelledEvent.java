package org.sparta.user.infrastructure.event.publisher;

import org.sparta.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Order 모듈로 전달:
 * - 예약된 포인트가 취소되었음을 알림
 * - Order 모듈에서 포인트 취소가 완료되었음을 인지
 */
public record PointReservationCancelledEvent(
        UUID eventId,
        UUID orderId,
        Instant occurredAt
) implements DomainEvent {

    public static PointReservationCancelledEvent of(UUID orderId) {
        return new PointReservationCancelledEvent(
                UUID.randomUUID(),
                orderId,
                Instant.now()
        );
    }
}
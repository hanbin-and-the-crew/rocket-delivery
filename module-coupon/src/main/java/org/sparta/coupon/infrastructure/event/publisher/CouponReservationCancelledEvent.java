package org.sparta.coupon.infrastructure.event.publisher;

import org.sparta.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Order 모듈로 전달:
 * - 예약된 쿠폰이 취소되었음을 알림
 * - Order 모듈에서 쿠폰 취소가 완료되었음을 인지
 */
public record CouponReservationCancelledEvent(
        UUID eventId,
        UUID orderId,
        UUID couponId,
        Instant occurredAt
) implements DomainEvent {

    public static CouponReservationCancelledEvent of(UUID orderId, UUID couponId) {
        return new CouponReservationCancelledEvent(
                UUID.randomUUID(),
                orderId,
                couponId,
                Instant.now()
        );
    }
}
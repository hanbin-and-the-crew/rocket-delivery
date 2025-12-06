package org.sparta.coupon.infrastructure.event.publisher;

import org.sparta.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Order 모듈로 전달:
 * - 예약된 쿠폰이 실제로 사용 확정되었음을 알림
 * - Order 모듈에서 쿠폰 할인이 최종 확정되었음을 인지
 */
public record CouponConfirmedEvent(
        UUID eventId,
        UUID orderId,
        UUID couponId,
        Long discountAmount,
        Instant occurredAt
) implements DomainEvent {

    public static CouponConfirmedEvent of(UUID orderId, UUID couponId, Long discountAmount) {
        return new CouponConfirmedEvent(
                UUID.randomUUID(),
                orderId,
                couponId,
                discountAmount,
                Instant.now()
        );
    }
}
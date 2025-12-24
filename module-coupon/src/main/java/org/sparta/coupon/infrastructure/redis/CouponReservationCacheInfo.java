package org.sparta.coupon.infrastructure.redis;

import java.util.UUID;

/**
 * Redis에 저장되는 쿠폰 예약 캐시 데이터
 */
public record CouponReservationCacheInfo(
        UUID reservationId,
        UUID couponId,
        UUID orderId,
        UUID userId
) {
}

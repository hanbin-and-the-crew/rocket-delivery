package org.sparta.coupon.support.fixtures;

import org.sparta.coupon.domain.entity.CouponReservation;

import java.util.UUID;




public final class CouponReservationFixture {

    private CouponReservationFixture() {}

    public static CouponReservation defaultReservation() {
        return CouponReservation.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                50000L,
                5000L
        );
    }
    public static CouponReservation withCouponAndOrder(UUID couponId, UUID orderId) {
        return CouponReservation.create(
                couponId,
                orderId,
                UUID.randomUUID(),
                50000L,
                5000L
        );
    }
}
package org.sparta.coupon.support.fixtures;

import org.sparta.coupon.domain.entity.Coupon;
import org.sparta.coupon.domain.enums.DiscountType;

import java.time.LocalDateTime;
import java.util.UUID;





public final class CouponFixture {

    private CouponFixture() {}

    public static Coupon defaultCoupon() {
        return Coupon.create(
                "TEST-COUPON-001",
                "테스트 쿠폰",
                DiscountType.FIXED,
                5000L,
                30000L,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30),
                UUID.randomUUID()
        );
    }

    public static Coupon withUserId(UUID userId) {
        return Coupon.create(
                "TEST-COUPON-002",
                "사용자 쿠폰",
                DiscountType.FIXED,
                5000L,
                30000L,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30),
                userId
        );
    }

    public static Coupon percentageCoupon() {
        return Coupon.create(
                "TEST-COUPON-003",
                "비율 할인 쿠폰",
                DiscountType.PERCENTAGE,
                10L,
                30000L,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30),
                UUID.randomUUID()
        );
    }

    public static Coupon expiredCoupon() {
        return Coupon.create(
                "TEST-COUPON-004",
                "만료된 쿠폰",
                DiscountType.FIXED,
                5000L,
                30000L,
                LocalDateTime.now().minusDays(30),
                LocalDateTime.now().minusDays(1),
                UUID.randomUUID()
        );
    }

    public static Coupon notStartedCoupon() {
        return Coupon.create(
                "TEST-COUPON-007",
                "시작 전 쿠폰",
                DiscountType.FIXED,
                5000L,
                30000L,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(30),
                UUID.randomUUID()
        );
    }

    public static Coupon withMinOrderAmount(Long minOrderAmount) {
        return Coupon.create(
                "TEST-COUPON-005",
                "최소 주문 금액 쿠폰",
                DiscountType.FIXED,
                5000L,
                minOrderAmount,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30),
                UUID.randomUUID()
        );
    }
    public static Coupon withDiscountAmount(Long discountAmount) {
        return Coupon.create(
                "TEST-COUPON-006",
                "할인 금액 쿠폰",
                DiscountType.FIXED,
                discountAmount,
                30000L,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30),
                UUID.randomUUID()
        );
    }
}

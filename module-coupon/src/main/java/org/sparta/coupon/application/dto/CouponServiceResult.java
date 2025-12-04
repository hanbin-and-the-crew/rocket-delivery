package org.sparta.coupon.application.dto;

import org.sparta.coupon.domain.enums.DiscountType;

import java.time.LocalDateTime;
import java.util.UUID;

public class CouponServiceResult {

    /**
     * 쿠폰 예약 결과
     */
    public record Reserve(
            UUID reservationId,
            Long discountAmount,
            DiscountType discountType,
            LocalDateTime expiresAt
    ) {}
}
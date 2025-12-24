package org.sparta.coupon.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 쿠폰 상태
 */
@Getter
@RequiredArgsConstructor
public enum CouponStatus {

    AVAILABLE("사용 가능"),
    RESERVED("예약됨"),
    PAID("사용 완료"),
    CANCELLED("취소됨"),
    EXPIRED("만료됨");

    private final String description;
}
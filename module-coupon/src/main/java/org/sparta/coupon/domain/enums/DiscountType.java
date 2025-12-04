package org.sparta.coupon.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 할인 타입
 */
@Getter
@RequiredArgsConstructor
public enum DiscountType {

    FIXED("고정 금액 할인"),
    PERCENTAGE("비율 할인");

    private final String description;
}
package org.sparta.order.domain.vo;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductMoney {

    private Long amount;

    private ProductMoney(Long amount) {
        if (amount == null) {
            throw new IllegalArgumentException("금액은 필수입니다");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("금액은 음수일 수 없습니다");
        }
        this.amount = amount;
    }

    public static ProductMoney of(Long amount) {
        return new ProductMoney(amount);
    }

    public static ProductMoney zero() {
        return new ProductMoney(0L);
    }

}

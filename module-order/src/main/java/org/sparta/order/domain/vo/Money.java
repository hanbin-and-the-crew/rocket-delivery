package org.sparta.order.domain.vo;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Money {

    private Long amount;

    private Money(Long amount) {
        if (amount == null) {
            throw new IllegalArgumentException("금액은 필수입니다.");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("금액은 음수일 수 없습니다.");
        }
        this.amount = amount;
    }

    public static Money of(Long amount) {
        return new Money(amount);
    }

    public static Money zero() {
        return new Money(0L);
    }

    /**
     * 단일 수량(int)과 곱해서 총 금액 계산
     */
    public Money multiply(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("수량은 음수일 수 없습니다.");
        }
        long result = Math.multiplyExact(this.amount, (long) quantity);
        return Money.of(result);
    }

    /**
     * Quantity VO와 곱해서 총 금액 계산
     */
    public Money multiply(Quantity quantity) {
        if (quantity == null) {
            throw new IllegalArgumentException("수량은 필수입니다.");
        }
        return multiply(quantity.getValue());
    }

    /**
     * 다른 Money와 더하기 (필요하면 사용)
     */
    public Money add(Money other) {
        if (other == null) {
            throw new IllegalArgumentException("더할 금액은 필수입니다.");
        }
        long result = Math.addExact(this.amount, other.amount);
        return Money.of(result);
    }
}

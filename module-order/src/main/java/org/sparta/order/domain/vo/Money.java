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

    /**
     * Creates a Money instance representing an amount of zero.
     *
     * @return a Money whose amount is 0
     */
    public static Money zero() {
        return new Money(0L);
    }

    /**
     * Calculates the total amount by multiplying this Money's amount by the specified quantity.
     *
     * @param quantity the number of units to multiply; must be greater than or equal to 0
     * @return a new Money equal to this amount multiplied by the given quantity
     * @throws IllegalArgumentException if {@code quantity} is negative
     */
    public Money multiply(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("수량은 음수일 수 없습니다.");
        }
        long result = Math.multiplyExact(this.amount, (long) quantity);
        return Money.of(result);
    }

    /**
     * Compute total money by multiplying this Money by a Quantity.
     *
     * @param quantity the quantity to multiply with; must not be null
     * @return a Money representing this amount multiplied by the quantity's value
     * @throws IllegalArgumentException if {@code quantity} is null
     */
    public Money multiply(Quantity quantity) {
        if (quantity == null) {
            throw new IllegalArgumentException("수량은 필수입니다.");
        }
        return multiply(quantity.getValue());
    }

    /**
     * Adds the specified Money amount to this Money.
     *
     * @param other the Money to add
     * @return a Money representing the sum of this Money and {@code other}
     * @throws IllegalArgumentException if {@code other} is {@code null}
     */
    public Money add(Money other) {
        if (other == null) {
            throw new IllegalArgumentException("더할 금액은 필수입니다.");
        }
        long result = Math.addExact(this.amount, other.amount);
        return Money.of(result);
    }
}
package org.sparta.order.domain.vo;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Quantity {

    private int value;

    private Quantity (int value) {

        if(value < 1) {throw new IllegalArgumentException("주문 수량은 최소 1개 이상이어야 합니다.");}

        this.value = value;
    }

    /**
     * Create a Quantity with the specified numeric amount.
     *
     * <p>The value must be greater than or equal to 1; otherwise an {@link IllegalArgumentException} is thrown.</p>
     *
     * @param value the desired quantity amount (must be >= 1)
     * @return the created {@code Quantity} instance
     * @throws IllegalArgumentException if {@code value} is less than 1
     */
    public static Quantity of(int value) {
        return new Quantity(value);
    }

}
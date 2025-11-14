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

        // if(value < 1) {throw new IllegalArgumentException("주문 수량은 최소 1개 이상이어야 합니다.");}
        // 해당 branch에선 오류를 일부러 발생시키기 위해 닫아놓음

        this.value = value;
    }

    public static Quantity of(int value) {
        return new Quantity(value);
    }

    public static Quantity zero() {
        return new Quantity(0);
    }
}

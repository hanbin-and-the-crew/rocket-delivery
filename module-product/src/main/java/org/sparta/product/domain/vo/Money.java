package org.sparta.product.domain.vo;

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
            throw new IllegalArgumentException("금액은 필수입니다");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("금액은 음수일 수 없습니다");
        }
        this.amount = amount;
    }

    public static Money of(Long amount) {
        return new Money(amount);
    }

    public static Money zero() {
        return new Money(0L);
    }

}

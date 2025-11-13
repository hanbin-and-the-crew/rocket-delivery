package org.sparta.delivery.domain.vo;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 소요 시간 Value Object - 단위: 분
 */
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Duration {

    private Integer value;

    private Duration(Integer value) {
        if (value == null) {
            throw new IllegalArgumentException("소요 시간은 필수입니다.");
        }
        if (value < 0) {
            throw new IllegalArgumentException("소요 시간은 음수일 수 없습니다.");
        }
        this.value = value;
    }

    public static Duration of(Integer value) {
        return new Duration(value);
    }

    public static Duration zero() {
        return new Duration(0);
    }
}

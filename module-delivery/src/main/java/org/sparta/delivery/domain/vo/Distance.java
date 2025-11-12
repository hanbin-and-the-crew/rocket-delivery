package org.sparta.delivery.domain.vo;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 거리 Value Object - 단위: km
 */
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Distance {

    private Double value;

    private Distance(Double value) {
        if (value == null) {
            throw new IllegalArgumentException("거리는 필수입니다.");
        }
        if (value < 0) {
            throw new IllegalArgumentException("거리는 음수일 수 없습니다.");
        }
        this.value = value;
    }

    public static Distance of(Double value) {
        return new Distance(value);
    }

    public static Distance zero() {
        return new Distance(0.0);
    }
}

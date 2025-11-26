package org.sparta.deliverylog.domain.vo;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 거리 Value Object - 단위: km
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Distance implements Serializable {

    private Double value;  // km

    private Distance(Double value) {
        this.value = value;
    }

    public static Distance of(Double value) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException("거리는 음수일 수 없습니다");
        }
        return new Distance(value);
    }

    public String format() {
        return value != null ? String.format("%.2f km", value) : "0.00 km";
    }
}
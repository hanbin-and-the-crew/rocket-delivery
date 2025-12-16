package org.sparta.deliverylog.domain.vo;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 소요 시간 Value Object - 단위: 분
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Duration implements Serializable {

    private Integer value;  // minutes

    private Duration(Integer value) {
        this.value = value;
    }

    public static Duration of(Integer value) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException("시간은 음수일 수 없습니다");
        }
        return new Duration(value);
    }

    public String format() {
        if (value == null) return "0분";
        int hours = value / 60;
        int minutes = value % 60;
        return hours > 0 ? String.format("%d시간 %d분", hours, minutes) : String.format("%d분", minutes);
    }
}
package org.sparta.order.domain.vo;


import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DueAtValue {

    private LocalDateTime time;

    public static DueAtValue of(LocalDateTime t) {
        validate(t);
        return new DueAtValue(t);
    }

    private static void validate(LocalDateTime t) {
        if (t == null) {
            throw new IllegalArgumentException(
                    org.sparta.order.domain.error.OrderErrorType.DUE_AT_REQUIRED.getMessage()
            );
        }
        if (t.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("납품 기한은 현재 시간 이후여야 합니다");
        }
    }
}
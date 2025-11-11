package org.sparta.slack.domain.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 최종 발송 시한 Value Object
 */
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Deadline {

    @Column(name = "deadline", nullable = false)
    private LocalDateTime value;

    private Deadline(LocalDateTime value) {
        if (value == null) {
            throw new IllegalArgumentException("발송 시한은 필수입니다");
        }
        this.value = value;
    }

    public static Deadline of(LocalDateTime value) {
        return new Deadline(value);
    }

    public boolean isPast() {
        return value.isBefore(LocalDateTime.now());
    }

    public boolean isFuture() {
        return value.isAfter(LocalDateTime.now());
    }
}
package org.sparta.slack.domain.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 발송 결과 Value Object
 */
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryResult {

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    private DeliveryResult(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public static DeliveryResult success() {
        return new DeliveryResult(null, null);
    }

    public static DeliveryResult failure(String errorCode, String errorMessage) {
        if (errorCode == null || errorCode.isBlank()) {
            throw new IllegalArgumentException("에러 코드는 필수입니다");
        }
        return new DeliveryResult(errorCode, errorMessage);
    }

    public boolean isSuccess() {
        return errorCode == null;
    }

    public boolean isFailure() {
        return errorCode != null;
    }
}
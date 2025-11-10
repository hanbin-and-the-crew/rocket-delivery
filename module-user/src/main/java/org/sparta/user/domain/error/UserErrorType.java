package org.sparta.user.domain.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sparta.common.error.ErrorType;
import org.springframework.http.HttpStatus;

import java.util.Locale;

@Getter
@RequiredArgsConstructor
public enum UserErrorType implements ErrorType {

    // 기본 에러
    // 4xx
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "Invalid request"),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Validation failed"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Unauthorized"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Forbidden"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),

    // 5xx
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),
    EXTERNAL_SYSTEM_ERROR(HttpStatus.BAD_GATEWAY, "External system error"),

    // User 검증 에러
    USER_NOT_FOUND(HttpStatus.BAD_REQUEST, "회원이 존재하지 않습니다."),
    USERNAME_REQUIRED(HttpStatus.BAD_REQUEST, "username은 필수입니다."),
    PASSWORD_REQUIRED(HttpStatus.BAD_REQUEST, "password는 필수입니다."),
    EMAIL_REQUIRED(HttpStatus.BAD_REQUEST, "email은 필수입니다."),
    SLACK_ID_REQUIRED(HttpStatus.BAD_REQUEST, "슬랙 ID는 필수입니다."),
    HUB_ID_REQUIRED(HttpStatus.BAD_REQUEST, "허브 ID는 필수입니다."),
    INVALID_STATUS_CHANGE(HttpStatus.BAD_REQUEST, "대기중인 회원만 상태를 변경할 수 있습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    UserErrorType(HttpStatus status, String message) {
        this.status = status;
        this.code = "product:" + name().toLowerCase(Locale.ROOT);
        this.message = message;
    }

}
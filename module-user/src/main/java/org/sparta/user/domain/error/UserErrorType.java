package org.sparta.user.domain.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sparta.common.error.ErrorType;
import org.springframework.http.HttpStatus;

import java.util.Locale;

@Getter
@RequiredArgsConstructor
public enum UserErrorType implements ErrorType {

    // User 검증 에러
    USERNAME_REQUIRED(HttpStatus.BAD_REQUEST, "username은 필수입니다."),
    PASSWORD_REQUIRED(HttpStatus.BAD_REQUEST, "password는 필수입니다."),
    EMAIL_REQUIRED(HttpStatus.BAD_REQUEST, "email은 필수입니다."),
    SLACK_ID_REQUIRED(HttpStatus.BAD_REQUEST, "슬랙 ID는 필수입니다."),
    HUB_ID_REQUIRED(HttpStatus.BAD_REQUEST, "허브 ID는 필수입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    UserErrorType(HttpStatus status, String message) {
        this.status = status;
        this.code = "product:" + name().toLowerCase(Locale.ROOT);
        this.message = message;
    }

}
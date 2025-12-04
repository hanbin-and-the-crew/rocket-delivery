package org.sparta.user.domain.error;


import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sparta.common.error.ErrorType;
import org.springframework.http.HttpStatus;

import java.util.Locale;

@Getter
@RequiredArgsConstructor
public enum PointErrorType implements ErrorType {

    POINT_NOT_FOUND(HttpStatus.NOT_FOUND, "포인트를 찾을 수 없습니다."),
    POINT_IS_INSUFFICIENT(HttpStatus.BAD_REQUEST, "사용 가능한 포인트가 부족합니다."),
    DUPLICATE_ORDER_ID(HttpStatus.BAD_REQUEST, "이미 해당 주문에 대한 포인트 예약이 존재합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    PointErrorType(HttpStatus status, String message) {
        this.status = status;
        this.code = "point:" + name().toLowerCase(Locale.ROOT);
        this.message = message;
    }

}
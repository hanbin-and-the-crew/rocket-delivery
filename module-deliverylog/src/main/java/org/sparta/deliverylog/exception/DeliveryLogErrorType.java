package org.sparta.deliverylog.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sparta.common.error.ErrorType;
import org.springframework.http.HttpStatus;

import java.util.Locale;

@Getter
@RequiredArgsConstructor
public enum DeliveryLogErrorType implements ErrorType {

    DELIVERY_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "배송 경로를 찾을 수 없습니다"),
    DELIVERY_LOG_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 배송 경로입니다"),
    INVALID_DELIVERY_STATUS(HttpStatus.BAD_REQUEST, "잘못된 배송 상태입니다"),
    DELIVERY_MAN_NOT_ASSIGNED(HttpStatus.BAD_REQUEST, "배송 담당자가 배정되지 않았습니다"),
    INVALID_DELIVERY_OPERATION(HttpStatus.BAD_REQUEST, "현재 상태에서는 해당 작업을 수행할 수 없습니다"),
    DELIVERY_LOG_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "이미 완료된 배송입니다"),
    DELIVERY_LOG_ALREADY_CANCELED(HttpStatus.BAD_REQUEST, "이미 취소된 배송입니다");

    private final HttpStatus status;
    private final String code;
    private final String message;

    DeliveryLogErrorType(HttpStatus status, String message) {
        this.status = status;
        this.code = "order:" + name().toLowerCase(Locale.ROOT);
        this.message = message;
    }
}

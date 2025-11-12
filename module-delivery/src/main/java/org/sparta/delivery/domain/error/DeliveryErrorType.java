package org.sparta.delivery.domain.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sparta.common.error.ErrorType;
import org.springframework.http.HttpStatus;

import java.util.Locale;

@Getter
@RequiredArgsConstructor
public enum DeliveryErrorType implements ErrorType {

    DELIVERY_NOT_FOUND(HttpStatus.NOT_FOUND, "배송을 찾을 수 없습니다"),
    DELIVERY_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 배송이 생성되어 있습니다"),
    INVALID_DELIVERY_STATUS(HttpStatus.BAD_REQUEST, "잘못된 배송 상태입니다"),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다"),
    HUB_NOT_FOUND(HttpStatus.NOT_FOUND, "허브를 찾을 수 없습니다"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),
    UNAUTHORIZED(HttpStatus.FORBIDDEN, "권한이 없습니다"),
    DELIVERY_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "이미 삭제된 배송입니다."),
    CANNOT_UPDATE_DELIVERY(HttpStatus.BAD_REQUEST, "배송을 수정할 수 없습니다."),
    CANNOT_DELETE_DELIVERY(HttpStatus.BAD_REQUEST, "배송을 삭제할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    DeliveryErrorType(HttpStatus status, String message) {
        this.status = status;
        this.code = "order:" + name().toLowerCase(Locale.ROOT);
        this.message = message;
    }
}

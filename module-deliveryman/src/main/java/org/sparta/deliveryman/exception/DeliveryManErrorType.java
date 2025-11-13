package org.sparta.deliveryman.exception;

import lombok.Getter;
import org.sparta.common.error.ErrorType;
import org.springframework.http.HttpStatus;

import java.util.Locale;

@Getter
public enum DeliveryManErrorType implements ErrorType {

    DELIVERY_MAN_NOT_FOUND(HttpStatus.NOT_FOUND, "배송 담당자를 찾을 수 없습니다."),
    DELIVERY_MAN_ALREADY_EXISTS(HttpStatus.CONFLICT, "동일한 배송 담당자가 이미 존재합니다."),
    INVALID_DELIVERY_MAN_STATUS(HttpStatus.BAD_REQUEST, "유효하지 않은 배송 담당자 상태입니다."),
    DELIVERY_MAN_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "배송 담당자 업데이트에 실패했습니다."),
    DELIVERY_MAN_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "배송 담당자 삭제에 실패했습니다."),
    DELIVERY_MAN_ASSIGNMENT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "배송 담당자 배정에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    DeliveryManErrorType(HttpStatus status, String message) {
        this.status = status;
        this.code = "deliveryman:" + name().toLowerCase(Locale.ROOT);
        this.message = message;
    }

}

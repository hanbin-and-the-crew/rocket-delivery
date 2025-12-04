package org.sparta.order.application.error;

import lombok.Getter;
import org.sparta.common.error.ErrorType;
import org.springframework.http.HttpStatus;

import java.util.Locale;

@Getter
public enum OrderApplicationErrorType implements ErrorType {

    // 데이터 무결성
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다"),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다"),
    SUPPLIER_NOT_FOUND(HttpStatus.NOT_FOUND, "공급업체를 찾을 수 없습니다"),
    RECEIPT_COMPANY_NOT_FOUND(HttpStatus.NOT_FOUND, "수령업체를 찾을 수 없습니다"),
    HUB_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 허브를 찾을 수 없습니다."),
    COMPANY_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 업체를 찾을 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;

    OrderApplicationErrorType(HttpStatus status, String message) {
        this.status = status;
        this.code = "order:" + name().toLowerCase(Locale.ROOT);
        this.message = message;
    }
}
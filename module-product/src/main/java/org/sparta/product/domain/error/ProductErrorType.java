package org.sparta.product.domain.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sparta.common.error.ErrorType;
import org.springframework.http.HttpStatus;

import java.util.Locale;

@Getter
@RequiredArgsConstructor
public enum ProductErrorType implements ErrorType {

    // Product 검증 에러
    PRODUCT_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "상품명은 필수입니다"),
    PRICE_REQUIRED(HttpStatus.BAD_REQUEST, "가격은 필수입니다"),
    CATEGORY_ID_REQUIRED(HttpStatus.BAD_REQUEST, "카테고리 ID는 필수입니다"),
    COMPANY_ID_REQUIRED(HttpStatus.BAD_REQUEST, "업체 ID는 필수입니다"),
    HUB_ID_REQUIRED(HttpStatus.BAD_REQUEST, "허브 ID는 필수입니다"),
    INITIAL_QUANTITY_INVALID(HttpStatus.BAD_REQUEST, "재고량은 0 이상이어야 합니다"),

    // Stock 검증 에러
    PRODUCT_REQUIRED(HttpStatus.BAD_REQUEST, "상품 정보는 필수입니다"),

    // Stock 비즈니스 에러
    INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST, "재고가 부족합니다"),
    STOCK_UNAVAILABLE(HttpStatus.BAD_REQUEST, "판매 불가 상태인 상품입니다"),
    DECREASE_QUANTITY_INVALID(HttpStatus.BAD_REQUEST, "차감 수량은 1 이상이어야 합니다"),
    INCREASE_QUANTITY_INVALID(HttpStatus.BAD_REQUEST, "증가 수량은 1 이상이어야 합니다"),

    // Stock 예약 비즈니스 에러
    RESERVE_QUANTITY_INVALID(HttpStatus.BAD_REQUEST, "예약 수량은 1 이상이어야 합니다"),
    INVALID_RESERVATION_CONFIRM(HttpStatus.BAD_REQUEST, "예약된 재고보다 많은 수량을 확정할 수 없습니다"),
    INVALID_RESERVATION_CANCEL(HttpStatus.BAD_REQUEST, "예약된 재고보다 많은 수량을 취소할 수 없습니다"),

    // Product 조회 에러
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다"),
    STOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "재고 정보를 찾을 수 없습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ProductErrorType(HttpStatus status, String message) {
        this.status = status;
        this.code = "product:" + name().toLowerCase(Locale.ROOT);
        this.message = message;
    }

}
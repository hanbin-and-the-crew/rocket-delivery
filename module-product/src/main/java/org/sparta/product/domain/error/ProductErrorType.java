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
    PRODUCT_REQUIRED(HttpStatus.BAD_REQUEST, "상품 정보는 필수입니다");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ProductErrorType(HttpStatus status, String message) {
        this.status = status;
        this.code = "product:" + name().toLowerCase(Locale.ROOT);
        this.message = message;
    }

}
package org.sparta.coupon.domain.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sparta.common.error.ErrorType;
import org.springframework.http.HttpStatus;

import java.util.Locale;

@Getter
@RequiredArgsConstructor
public enum CouponErrorType implements ErrorType {

    // Coupon 조회 에러
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "쿠폰을 찾을 수 없습니다"),

    // Coupon 검증 에러
    COUPON_ALREADY_USED(HttpStatus.BAD_REQUEST, "이미 사용된 쿠폰입니다"),
    COUPON_EXPIRED(HttpStatus.BAD_REQUEST, "만료된 쿠폰입니다"),
    COUPON_NOT_STARTED(HttpStatus.BAD_REQUEST, "시작 전인 쿠폰입니다"),
    COUPON_INVALID_STATUS(HttpStatus.BAD_REQUEST, "쿠폰 상태가 올바르지 않습니다"),
    INSUFFICIENT_ORDER_AMOUNT(HttpStatus.BAD_REQUEST, "최소 주문 금액을 만족하지 않습니다"),
    USER_NOT_OWNER(HttpStatus.FORBIDDEN, "쿠폰 소유자가 아닙니다"),

    // Reservation 에러
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "예약 정보를 찾을 수 없습니다"),
    RESERVATION_EXPIRED(HttpStatus.BAD_REQUEST, "예약이 만료되었습니다"),
    INVALID_ORDER(HttpStatus.BAD_REQUEST, "주문 정보가 일치하지 않습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;

    CouponErrorType(HttpStatus status, String message) {
        this.status = status;
        this.code = "coupon:" + name().toLowerCase(Locale.ROOT);
        this.message = message;
    }
}

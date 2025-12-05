package org.sparta.order.domain.error;

import lombok.Getter;
import org.sparta.common.error.ErrorType;
import org.springframework.http.HttpStatus;

import java.util.Locale;
@Getter
public enum OrderErrorType implements ErrorType {

    // 필수값 누락
    CUSTOMER_ID_REQUIRED(HttpStatus.BAD_REQUEST, "주문자 ID는 필수입니다"),
    SUPPLIER_COMPANY_ID_REQUIRED(HttpStatus.BAD_REQUEST, "요청업체 ID는 필수입니다"),
    SUPPLIER_HUB_ID_REQUIRED(HttpStatus.BAD_REQUEST, "요청업체 허브 ID는 필수입니다"),
    RECEIPT_COMPANY_ID_REQUIRED(HttpStatus.BAD_REQUEST, "수령업체 ID는 필수입니다"),
    RECEIPT_HUB_ID_REQUIRED(HttpStatus.BAD_REQUEST, "수령업체 허브 ID는 필수입니다"),
    PRODUCT_ID_REQUIRED(HttpStatus.BAD_REQUEST, "상품 ID는 필수입니다"),
    PRODUCT_NAME_SNAPSHOT_REQUIRED(HttpStatus.BAD_REQUEST, "상품명 스냅샷은 필수입니다"),
    PRODUCT_PRICE_SNAPSHOT_REQUIRED(HttpStatus.BAD_REQUEST, "상품 가격 스냅샷은 필수입니다"),
    QUANTITY_REQUIRED(HttpStatus.BAD_REQUEST, "주문 수량은 필수입니다"),
    ADDRESS_SNAPSHOT_REQUIRED(HttpStatus.BAD_REQUEST, "주소는 필수입니다"),
    DUE_AT_REQUIRED(HttpStatus.BAD_REQUEST, "납품 기한은 필수입니다"),
    USERNAME_REQUIRED(HttpStatus.BAD_REQUEST, "주문자 실명은 필수입니다"),
    USER_PHONE_NUMBER_REQUIRED(HttpStatus.BAD_REQUEST, "전화번호는 필수입니다"),
    SLACK_ID_REQUIRED(HttpStatus.BAD_REQUEST, "slack 아이디는 필수입니다"),
    USER_ID_REQUIRED(HttpStatus.BAD_REQUEST, "사용자 ID는 필수입니다"),
    DELIVERY_ID_REQUIRED(HttpStatus.BAD_REQUEST, "배송 ID는 필수입니다"),
    SHIPPED_AT_REQUIRED(HttpStatus.BAD_REQUEST, "출고 시간은 필수입니다"),
    CANCELED_REASON_CODE_REQUIRED(HttpStatus.BAD_REQUEST, "취소 사유 코드는 필수입니다"),
    CANCELED_REASON_MEMO_REQUIRED(HttpStatus.BAD_REQUEST, "취소 사유 상세는 필수입니다"),

    // 예약
    STOCK_RESERVATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "재고 예약 실패"),
    POINT_RESERVATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "포인트 예약 실패"),
    COUPON_RESERVATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "쿠폰 예약 실패"),
    COUPON_VALIDATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "쿠폰 검증 실패"),
    PAYMENT_APPROVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "결제 승인 실패"),

    // 상태 충돌 - 배송 출발(출고) 관련
    ORDER_ALREADY_SHIPPED(HttpStatus.CONFLICT, "이미 출고된 주문입니다"),
    CANNOT_SHIPPED_CANCELED_ORDER(HttpStatus.CONFLICT, "취소된 주문은 출고할 수 없습니다"),
    CANNOT_SHIPPED_DELIVERED_ORDER(HttpStatus.CONFLICT, "이미 배송 완료된 주문입니다"),

    // 상태 충돌 - 취소 관련
    ORDER_ALREADY_CANCELED(HttpStatus.CONFLICT, "이미 취소된 주문입니다"),
    CANNOT_CANCEL_SHIPPED_ORDER(HttpStatus.CONFLICT, "출고된 주문은 취소할 수 없습니다"),
    CANNOT_CANCEL_DELIVERED_ORDER(HttpStatus.CONFLICT, "배송 완료된 주문은 취소할 수 없습니다"),

    // 상태 충돌 - 수정 관련
    CANNOT_CHANGE_NOT_CREATED_ORDER(HttpStatus.CONFLICT, "주문 상태가 CREATED일 때만 수정할 수 있습니다"),
    CANNOT_CHANGE_DUE_AT_AFTER_SHIPPED(HttpStatus.CONFLICT, "출고 후에는 납기일을 변경할 수 없습니다"),
    CANNOT_CHANGE_MEMO_AFTER_SHIPPED(HttpStatus.CONFLICT, "출고 후에는 요청사항을 변경할 수 없습니다"),
    CANNOT_CHANGE_ADDRESS_AFTER_SHIPPED(HttpStatus.CONFLICT, "출고 후에는 주소를 변경할 수 없습니다."),

    // 비즈니스 규칙 위반
    INVALID_QUANTITY_RANGE(HttpStatus.BAD_REQUEST, "주문 수량은 최소 1개 이상이어야 합니다"),
    INVALID_TOTAL_PRICE(HttpStatus.BAD_REQUEST, "총 금액 계산 중 오류가 발생했습니다"),
    UNAUTHORIZED_USER_SUPPLIER_ID(HttpStatus.BAD_REQUEST, "사용자와 주문자가 일치하지 않습니다."),

    // 배송 관련
    DELIVERY_CREATED_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "배송 생성에 실패했습니다"),
    DELIVERY_LOG_CREATED_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "배송로그 생성에 실패했습니다"),
    DELIVERY_LOG_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "배송로그를 배송에 저장 실패"),
    DELIVERY_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "배송 삭제 실패"),

    // ===== 여기부터 새로 추가 =====
    CANNOT_SHIP_NOT_APPROVED_ORDER(HttpStatus.CONFLICT, "APPROVED 상태에서만 출고할 수 있습니다"),
    CANNOT_DELIVER_NOT_SHIPPED_ORDER(HttpStatus.CONFLICT, "SHIPPED 상태에서만 배송 완료로 변경할 수 있습니다"),
    ORDER_ALREADY_DELIVERED(HttpStatus.CONFLICT, "이미 배송 완료된 주문입니다"),
    CANNOT_DELETE_SHIPPED_OR_DELIVERED_ORDER(HttpStatus.CONFLICT, "출고되었거나 배송 완료된 주문은 삭제할 수 없습니다"),

    // 멱등성
    REQUEST_IN_PROGRESS(HttpStatus.CONFLICT, "동일한 요청이 처리 중입니다. 잠시 후 다시 시도해주세요");

    private final HttpStatus status;
    private final String code;
    private final String message;

    OrderErrorType(HttpStatus status, String message) {
        this.status = status;
        this.code = "order:" + name().toLowerCase(Locale.ROOT);
        this.message = message;
    }
}

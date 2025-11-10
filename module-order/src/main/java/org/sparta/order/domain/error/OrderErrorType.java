package org.sparta.order.domain.error;

import lombok.Getter;
import org.sparta.common.error.ErrorType;
import org.springframework.http.HttpStatus;

import java.util.Locale;

@Getter
public enum OrderErrorType implements ErrorType {

    // 필수값 누락 - 주문 생성 시
    SUPPLIER_ID_REQUIRED(HttpStatus.BAD_REQUEST, "공급자 ID는 필수입니다"),
    SUPPLIER_COMPANY_ID_REQUIRED(HttpStatus.BAD_REQUEST, "요청업체 ID는 필수입니다"),
    SUPPLIER_HUB_ID_REQUIRED(HttpStatus.BAD_REQUEST, "요청업체 허브 ID는 필수입니다"),
    RECEIPT_COMPANY_ID_REQUIRED(HttpStatus.BAD_REQUEST, "수령업체 ID는 필수입니다"),
    RECEIPT_HUB_ID_REQUIRED(HttpStatus.BAD_REQUEST, "수령업체 허브 ID는 필수입니다"),
    PRODUCT_ID_REQUIRED(HttpStatus.BAD_REQUEST, "상품 ID는 필수입니다"),
    PRODUCT_NAME_SNAPSHOT_REQUIRED(HttpStatus.BAD_REQUEST, "상품명 스냅샷은 필수입니다"),
    PRODUCT_PRICE_SNAPSHOT_REQUIRED(HttpStatus.BAD_REQUEST, "상품 가격 스냅샷은 필수입니다"),
    QUANTITY_REQUIRED(HttpStatus.BAD_REQUEST, "주문 수량은 필수입니다"),
    ADDRESS_SNAPSHOT_REQUIRED(HttpStatus.BAD_REQUEST, "주소 스냅샷은 필수입니다"),
    DUE_AT_REQUIRED(HttpStatus.BAD_REQUEST, "납품 기한은 필수입니다"),

    // 필수값 누락 - 주문 작업 시
    ORDER_ID_REQUIRED(HttpStatus.BAD_REQUEST, "주문 ID는 필수입니다"),
    USER_ID_REQUIRED(HttpStatus.BAD_REQUEST, "사용자 ID는 필수입니다"),
    DELIVERY_ID_REQUIRED(HttpStatus.BAD_REQUEST, "배송 ID는 필수입니다"),
    DISPATCHED_AT_REQUIRED(HttpStatus.BAD_REQUEST, "출고 시간은 필수입니다"),
    CANCELED_REASON_CODE_REQUIRED(HttpStatus.BAD_REQUEST, "취소 사유 코드는 필수입니다"),
    CANCELED_REASON_MEMO_REQUIRED(HttpStatus.BAD_REQUEST, "취소 사유 상세는 필수입니다"),

    // 상태 충돌 - 출고 관련
    ORDER_ALREADY_DISPATCHED(HttpStatus.CONFLICT, "이미 출고된 주문입니다"),
    CANNOT_DISPATCH_CANCELED_ORDER(HttpStatus.CONFLICT, "취소된 주문은 출고할 수 없습니다"),
    CANNOT_DISPATCH_DELIVERED_ORDER(HttpStatus.CONFLICT, "이미 배송 완료된 주문입니다"),

    // 상태 충돌 - 취소 관련
    ORDER_ALREADY_CANCELED(HttpStatus.CONFLICT, "이미 취소된 주문입니다"),
    CANNOT_CANCEL_DISPATCHED_ORDER(HttpStatus.CONFLICT, "출고된 주문은 취소할 수 없습니다"),
    CANNOT_CANCEL_DELIVERED_ORDER(HttpStatus.CONFLICT, "배송 완료된 주문은 취소할 수 없습니다"),

    // 상태 충돌 - 수정 관련
    CANNOT_MODIFY_NOT_PLACED_ORDER(HttpStatus.CONFLICT, "주문 상태가 PLACED일 때만 수정할 수 있습니다"),
    CANNOT_CHANGE_QUANTITY_AFTER_DISPATCH(HttpStatus.CONFLICT, "출고 후에는 수량을 변경할 수 없습니다"),
    CANNOT_CHANGE_DUE_AT_AFTER_DISPATCH(HttpStatus.CONFLICT, "출고 후에는 납기일을 변경할 수 없습니다"),
    CANNOT_CHANGE_MEMO_AFTER_DISPATCH(HttpStatus.CONFLICT, "출고 후에는 요청사항을 변경할 수 없습니다"),

    // 비즈니스 규칙 위반
    INVALID_QUANTITY_RANGE(HttpStatus.BAD_REQUEST, "주문 수량은 최소 1개 이상이어야 합니다"),
    INVALID_TOTAL_PRICE(HttpStatus.BAD_REQUEST, "총 금액 계산 중 오류가 발생했습니다"),

    // 데이터 무결성
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다"),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다"),
    SUPPLIER_NOT_FOUND(HttpStatus.NOT_FOUND, "공급업체를 찾을 수 없습니다"),
    RECEIPT_COMPANY_NOT_FOUND(HttpStatus.NOT_FOUND, "수령업체를 찾을 수 없습니다"),
    HUB_NOT_FOUND(HttpStatus.NOT_FOUND, "허브를 찾을 수 없습니다."),
    COMPANY_NOT_FOUND(HttpStatus.NOT_FOUND, "업체를 찾을 수 없습니다."),

    // 배송 관련
    DELIVERY_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "배송 생성에 실패했습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;

    OrderErrorType(HttpStatus status, String message) {
        this.status = status;
        this.code = "order:" + name().toLowerCase(Locale.ROOT);
        this.message = message;
    }
}
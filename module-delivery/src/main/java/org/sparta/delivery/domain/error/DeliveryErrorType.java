package org.sparta.delivery.domain.error;

import lombok.Getter;
import org.sparta.common.error.ErrorType;
import org.springframework.http.HttpStatus;

import java.util.Locale;

@Getter
public enum DeliveryErrorType implements ErrorType {

    // ===== 공통 조회/권한 =====
    DELIVERY_NOT_FOUND(HttpStatus.NOT_FOUND, "배송을 찾을 수 없습니다."),
    DELIVERY_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "이미 삭제된 배송입니다."),

    // ===== 생성 / 필수값 검증 =====
    ORDER_ID_REQUIRED(HttpStatus.BAD_REQUEST, "주문 ID는 필수입니다."),
    CUSTOMER_ID_REQUIRED(HttpStatus.BAD_REQUEST, "고객 ID는 필수입니다."),
    SUPPLIER_COMPANY_ID_REQUIRED(HttpStatus.BAD_REQUEST, "공급(출고) 업체 ID는 필수입니다."),
    SUPPLIER_HUB_ID_REQUIRED(HttpStatus.BAD_REQUEST, "공급(출고) 허브 ID는 필수입니다."),
    RECEIVE_COMPANY_ID_REQUIRED(HttpStatus.BAD_REQUEST, "수령(납품) 업체 ID는 필수입니다."),
    RECEIVE_HUB_ID_REQUIRED(HttpStatus.BAD_REQUEST, "수령(납품) 허브 ID는 필수입니다."),
    ADDRESS_REQUIRED(HttpStatus.BAD_REQUEST, "배송지 주소는 필수입니다."),
    RECEIVER_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "수령인 이름은 필수입니다."),
    RECEIVER_PHONE_REQUIRED(HttpStatus.BAD_REQUEST, "수령인 전화번호는 필수입니다."),
    INVALID_TOTAL_LOG_SEQ(HttpStatus.BAD_REQUEST, "허브 경로 전체 개수(totalLogSeq)는 0 이상이어야 합니다."),
    CREATION_FAILED(HttpStatus.CREATED, "배송 생성을 실패했습니다."),
    DELIVERY_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "배송이 이미 존재합니다."),
    NO_ROUTE_AVAILABLE(HttpStatus.BAD_REQUEST, "가능한 허브 경로가 없습니다."),

    // ===== 담당자 배정 =====
    DELIVERY_MAN_ID_REQUIRED(HttpStatus.BAD_REQUEST, "배송 담당자 ID는 필수입니다."),
    INVALID_STATUS_FOR_HUB_ASSIGN(HttpStatus.BAD_REQUEST, "현재 배송 상태에서는 허브 담당자를 배정할 수 없습니다."),
    INVALID_STATUS_FOR_COMPANY_ASSIGN(HttpStatus.BAD_REQUEST, "현재 배송 상태에서는 업체 담당자를 배정할 수 없습니다."),

    // ===== 허브 leg 진행 =====
    INVALID_STATUS_FOR_HUB_START(HttpStatus.BAD_REQUEST, "현재 배송 상태에서는 허브 출발 처리를 할 수 없습니다."),
    INVALID_STATUS_FOR_HUB_COMPLETE(HttpStatus.BAD_REQUEST, "현재 배송 상태에서는 허브 도착 처리를 할 수 없습니다."),
    INVALID_LOG_SEQUENCE(HttpStatus.BAD_REQUEST, "현재 배송에서 허용되지 않는 시퀀스 번호입니다."),

    // ===== 업체 구간 진행 =====
    INVALID_STATUS_FOR_COMPANY_START(HttpStatus.BAD_REQUEST, "현재 배송 상태에서는 업체 배송을 시작할 수 없습니다."),
    INVALID_STATUS_FOR_COMPLETE(HttpStatus.BAD_REQUEST, "현재 배송 상태에서는 납품 완료 처리를 할 수 없습니다."),

    // ===== 취소/삭제 =====
    INVALID_STATUS_FOR_CANCEL(HttpStatus.BAD_REQUEST, "현재 배송 상태에서는 배송을 취소할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    DeliveryErrorType(HttpStatus status, String message) {
        this.status = status;
        this.code = "delivery:" + name().toLowerCase(Locale.ROOT);
        this.message = message;
    }
}

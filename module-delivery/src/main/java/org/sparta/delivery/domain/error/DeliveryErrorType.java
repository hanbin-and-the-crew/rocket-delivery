package org.sparta.delivery.domain.error;

import lombok.Getter;
import org.sparta.common.error.ErrorType;
import org.springframework.http.HttpStatus;

import java.util.Locale;

@Getter
public enum DeliveryErrorType implements ErrorType {

    // ===== 공통 조회/권한 =====
    DELIVERY_NOT_FOUND(HttpStatus.NOT_FOUND, "배송을 찾을 수 없습니다."),
    DELIVERY_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 배송이 생성되어 있습니다."),
    INVALID_DELIVERY_STATUS(HttpStatus.BAD_REQUEST, "잘못된 배송 상태입니다."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),
    HUB_NOT_FOUND(HttpStatus.NOT_FOUND, "허브를 찾을 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    UNAUTHORIZED(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    DELIVERY_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "이미 삭제된 배송입니다."),
    CANNOT_UPDATE_DELIVERY(HttpStatus.BAD_REQUEST, "배송을 수정할 수 없습니다."),
    CANNOT_DELETE_DELIVERY(HttpStatus.BAD_REQUEST, "배송을 삭제할 수 없습니다."),

    // ===== 생성 / 필수값 검증 =====
    ORDER_ID_REQUIRED(HttpStatus.BAD_REQUEST, "주문 ID는 필수입니다."),
    CUSTOMER_ID_REQUIRED(HttpStatus.BAD_REQUEST, "고객 ID는 필수입니다."),
    SUPPLIER_INFO_REQUIRED(HttpStatus.BAD_REQUEST, "공급(출고) 업체/허브 정보는 필수입니다."),
    RECEIVER_INFO_REQUIRED(HttpStatus.BAD_REQUEST, "수령(납품) 업체/허브 정보는 필수입니다."),
    ADDRESS_REQUIRED(HttpStatus.BAD_REQUEST, "배송지 주소는 필수입니다."),
    RECEIVER_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "수령인 이름은 필수입니다."),
    RECEIVER_PHONE_REQUIRED(HttpStatus.BAD_REQUEST, "수령인 전화번호는 필수입니다."),
    DUE_AT_REQUIRED(HttpStatus.BAD_REQUEST, "납기일은 필수입니다."),

    // ===== 허브/거리/시간 관련 (DeliveryLog 생성/검증) =====
    HUB_ID_REQUIRED(HttpStatus.BAD_REQUEST, "허브 ID는 필수입니다."),
    ESTIMATED_DISTANCE_MUST_BE_POSITIVE(HttpStatus.BAD_REQUEST, "예상 거리는 0보다 커야 합니다."),
    ESTIMATED_MINUTES_MUST_BE_POSITIVE(HttpStatus.BAD_REQUEST, "예상 소요 시간은 0보다 커야 합니다."),
    ACTUAL_DISTANCE_MUST_BE_POSITIVE(HttpStatus.BAD_REQUEST, "실제 이동 거리는 0보다 커야 합니다."),
    ACTUAL_MINUTES_MUST_BE_POSITIVE(HttpStatus.BAD_REQUEST, "실제 소요 시간은 0보다 커야 합니다."),

    // ===== 로그 조회 / 시퀀스 =====
    LOG_NOT_FOUND_FOR_SEQUENCE(HttpStatus.NOT_FOUND, "해당 시퀀스의 배송 로그를 찾을 수 없습니다."),

    // ===== 담당자 배정 / HUB_WAITING 전환 =====
    INVALID_STATUS_FOR_ASSIGNMENT(HttpStatus.BAD_REQUEST, "현재 배송 상태에서는 담당자 배정을 할 수 없습니다."),
    INVALID_LOG_STATUS_FOR_ASSIGNMENT(HttpStatus.BAD_REQUEST, "현재 배송 로그 상태에서는 HUB_WAITING으로 전환할 수 없습니다."),
    HUB_DELIVERYMAN_MISMATCH(HttpStatus.BAD_REQUEST, "배송 담당자가 일치하지 않습니다."),
    COMPANY_DELIVERYMAN_MISMATCH(HttpStatus.BAD_REQUEST, "업체 배송 담당자가 일치하지 않습니다."),

    // ===== 허브 출발 (HUB_WAITING → HUB_MOVING) =====
    DELIVERYMAN_ID_REQUIRED(HttpStatus.BAD_REQUEST, "배송 담당자 ID는 필수입니다."),
    INVALID_STATUS_FOR_HUB_DEPARTURE(HttpStatus.BAD_REQUEST, "현재 배송 상태에서는 허브 출발 처리를 할 수 없습니다."),
    INVALID_LOG_STATUS_FOR_HUB_DEPARTURE(HttpStatus.BAD_REQUEST, "현재 배송 로그 상태에서는 허브 출발 처리를 할 수 없습니다."),

    // ===== 허브 도착 (HUB_MOVING → HUB_ARRIVED / DEST_HUB_ARRIVED) =====
    INVALID_STATUS_FOR_HUB_ARRIVAL(HttpStatus.BAD_REQUEST, "현재 배송 상태에서는 허브 도착 처리를 할 수 없습니다."),
    INVALID_LOG_STATUS_FOR_HUB_ARRIVAL(HttpStatus.BAD_REQUEST, "현재 배송 로그 상태에서는 허브 도착 처리를 할 수 없습니다."),

    // ===== 목적지 허브 → 업체 이동 (DEST_HUB_ARRIVED → COMPANY_MOVING) =====
    INVALID_STATUS_FOR_COMPANY_MOVING(HttpStatus.BAD_REQUEST, "현재 배송 상태에서는 업체 배송 시작을 할 수 없습니다."),

    // ===== 업체 배송 완료 (COMPANY_MOVING → DELIVERED) =====
    DELIVERY_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "이미 배송이 완료된 건입니다."),
    INVALID_STATUS_FOR_DELIVERY_COMPLETE(HttpStatus.BAD_REQUEST, "현재 배송 상태에서는 납품 완료 처리를 할 수 없습니다."),

    // ===== 취소 (CREATED / HUB_WAITING 에서만 가능) =====
    INVALID_STATUS_FOR_CANCEL(HttpStatus.BAD_REQUEST, "현재 배송 상태에서는 배송을 취소할 수 없습니다."),
    CANNOT_CANCEL_WHILE_LEG_IN_PROGRESS(HttpStatus.BAD_REQUEST, "진행 중이거나 도착 처리된 허브 경로가 있어 배송을 취소할 수 없습니다."),
    INVALID_LOG_STATUS_FOR_CANCEL(HttpStatus.BAD_REQUEST, "현재 배송 로그 상태에서는 취소할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    DeliveryErrorType(HttpStatus status, String message) {
        this.status = status;
        this.code = "delivery:" + name().toLowerCase(Locale.ROOT);
        this.message = message;
    }
}

package org.sparta.deliveryman.domain.error;

import lombok.Getter;
import org.sparta.common.error.ErrorType;
import org.springframework.http.HttpStatus;

import java.util.Locale;

@Getter
public enum DeliveryManErrorType implements ErrorType {

    // ===== 생성 필수값 (UserCreatedEvent 기반) =====
    USER_ID_REQUIRED(HttpStatus.BAD_REQUEST, "User ID는 필수입니다."),
    REAL_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "담당자 실명(realName)은 필수입니다."),
    USER_ROLE_REQUIRED(HttpStatus.BAD_REQUEST, "사용자 권한(userRole)은 필수입니다."),
    USER_STATUS_REQUIRED(HttpStatus.BAD_REQUEST, "사용자 상태(userStatus)는 필수입니다."),
    TYPE_REQUIRED(HttpStatus.BAD_REQUEST, "배송 담당자 타입(type)은 필수입니다."),
    INVALID_SEQUENCE(HttpStatus.BAD_REQUEST, "sequence 값은 1 이상이어야 합니다."),
    HUB_ID_REQUIRED_FOR_COMPANY(HttpStatus.BAD_REQUEST, "업체 배송 담당자는 허브 Id가 필수 입니다."),
    SEQUENCE_REQUIRED(HttpStatus.BAD_REQUEST, "시퀀스 번호는 필수 입니다."),
    DELIVERY_MAN_NOT_FOUND(HttpStatus.BAD_REQUEST, "해당 담당자가 없습니다."),


    HUB_TYPE_MUST_NOT_HAVE_HUB_ID(HttpStatus.BAD_REQUEST, "HUB 타입 배송 담당자는 hubId를 가질 수 없습니다."),
    COMPANY_TYPE_MUST_HAVE_HUB_ID(HttpStatus.BAD_REQUEST, "COMPANY 타입 배송 담당자는 hubId가 반드시 있어야 합니다."),

    // ===== UserUpdateEvent 기반 정보 변경 =====
    NO_CHANGES_IN_USER_EVENT(HttpStatus.BAD_REQUEST, "변경할 사용자 정보가 없습니다."),
    NO_CHANGES_TO_UPDATE(HttpStatus.BAD_REQUEST, "변경할 내용이 없습니다."),

    // ===== 상태 변경 (컨트롤러/로직에서 직접 호출) =====
    STATUS_REQUIRED(HttpStatus.BAD_REQUEST, "변경할 상태는 필수입니다."),
    STATUS_ALREADY_SAME(HttpStatus.BAD_REQUEST, "현재 상태와 변경할 상태가 동일합니다."),
    CANNOT_CHANGE_STATUS_DELETED(HttpStatus.BAD_REQUEST, "DELETED 상태인 담당자의 상태는 변경할 수 없습니다."),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "허용되지 않는 상태 전이입니다."),

    // ===== 배정 로직 연관 =====
    CANNOT_ASSIGN_OFFLINE_OR_DELETED(HttpStatus.BAD_REQUEST, "OFFLINE 이거나 DELETED 상태인 담당자에게는 배송을 배정할 수 없습니다."),
    DELIVERY_COUNT_UNDERFLOW(HttpStatus.BAD_REQUEST, "deliveryCount가 0보다 작아질 수 없습니다."),
    NO_AVAILABLE_DELIVERY_MAN(HttpStatus.BAD_REQUEST,"배송 담당자로 배정할 수 없습니다."),
    NO_HUB_DELIVERY_MAN_AVAILABLE(HttpStatus.BAD_REQUEST, "배정할 허브 배송 담당자가 없습니다."),
    NO_COMPANY_DELIVERY_MAN_AVAILABLE(HttpStatus.BAD_REQUEST, "배정할 업체 배송 담당자가 없습니다."),


    // ===== SoftDelete =====
    ALREADY_SOFT_DELETED(HttpStatus.BAD_REQUEST, "이미 삭제 처리된 배송 담당자입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    DeliveryManErrorType(HttpStatus status, String message) {
        this.status = status;
        this.code = "deliveryman:" + name().toLowerCase(Locale.ROOT);
        this.message = message;
    }
}

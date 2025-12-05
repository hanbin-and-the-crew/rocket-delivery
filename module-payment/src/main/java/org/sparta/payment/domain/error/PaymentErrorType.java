package org.sparta.payment.domain.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sparta.common.error.ErrorType;
import org.springframework.http.HttpStatus;

import java.util.Locale;

@Getter
@RequiredArgsConstructor
public enum PaymentErrorType implements ErrorType {

    // ===========================
    // PAYMENT (결제)
    // ===========================
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "결제를 찾을 수 없습니다."),
    PAYMENT_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "이미 완료된 결제입니다."),
    PAYMENT_ALREADY_CANCELED(HttpStatus.BAD_REQUEST, "이미 취소된 결제입니다."),
    PAYMENT_ALREADY_REFUNDED(HttpStatus.BAD_REQUEST, "이미 전액 환불된 결제입니다."),
    PAYMENT_INVALID_STATUS(HttpStatus.BAD_REQUEST, "결제 상태가 유효하지 않습니다."),
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "결제 금액이 일치하지 않습니다."),
    PAYMENT_METHOD_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "지원하지 않는 결제 방식입니다."),
    PAYMENT_APPROVAL_FAILED(HttpStatus.BAD_GATEWAY, "PG 결제 승인에 실패했습니다."),
    PAYMENT_CANCEL_FAILED(HttpStatus.BAD_GATEWAY, "PG 결제 취소에 실패했습니다."),
    PAYMENT_INSUFFICIENT_AMOUNT(HttpStatus.BAD_REQUEST, "환불 가능한 금액이 부족합니다."),
    PAYMENT_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "이미 처리된 결제 요청입니다."),
    REFUND_AMOUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "환불 금액이 가능한 범위를 초과했습니다."),

    // ===========================
    // REFUND (환불)
    // ===========================
    REFUND_NOT_FOUND(HttpStatus.NOT_FOUND, "환불 이력을 찾을 수 없습니다."),
    REFUND_FAILED(HttpStatus.BAD_GATEWAY, "환불 처리 중 오류가 발생했습니다."),
    REFUND_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "이미 완료된 환불입니다."),
    REFUND_AMOUNT_INVALID(HttpStatus.BAD_REQUEST, "환불 금액이 잘못되었습니다."),
    REFUND_EXCEEDS_PAYMENT_AMOUNT(HttpStatus.BAD_REQUEST, "환불 금액이 결제 금액을 초과합니다."),

    // ===========================
    // PAYMENT ATTEMPT (PG 호출)
    // ===========================
    ATTEMPT_NOT_FOUND(HttpStatus.NOT_FOUND, "결제 시도 내역을 찾을 수 없습니다."),
    ATTEMPT_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "이미 완료된 결제 시도입니다."),
    ATTEMPT_PG_COMMUNICATION_FAILED(HttpStatus.BAD_GATEWAY, "PG와의 통신에 실패했습니다."),
    ATTEMPT_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "PG 응답 시간이 초과되었습니다."),

    // ===========================
    // OUTBOX
    // ===========================
    OUTBOX_NOT_FOUND(HttpStatus.NOT_FOUND, "Outbox 이벤트를 찾을 수 없습니다."),
    OUTBOX_ALREADY_SENT(HttpStatus.BAD_REQUEST, "이미 Kafka로 발행된 이벤트입니다."),
    OUTBOX_PUBLISH_FAILED(HttpStatus.BAD_GATEWAY, "Outbox 이벤트 발행 중 오류가 발생했습니다."),

    // ===========================
    // 공통 / VALIDATION
    // ===========================
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "유효하지 않은 요청입니다."),
    INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "금액이 유효한 값이 아닙니다."),
    ILLEGAL_STATE(HttpStatus.BAD_REQUEST, "현재 상태에서 수행할 수 없는 작업입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    PaymentErrorType(HttpStatus status, String message) {
        this.status = status;
        this.code = "payment:" + name().toLowerCase(Locale.ROOT);
        this.message = message;
    }
}

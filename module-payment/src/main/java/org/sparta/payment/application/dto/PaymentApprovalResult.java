package org.sparta.payment.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * PG 승인 mock 결과를 표현하는 DTO
 */
public record PaymentApprovalResult(
        UUID orderId,
        boolean approved,
        String paymentKey,
        LocalDateTime approvedAt,
        String failureCode,
        String failureMessage
) {

    public static PaymentApprovalResult success(UUID orderId, String paymentKey, LocalDateTime approvedAt) {
        return new PaymentApprovalResult(orderId, true, paymentKey, approvedAt, null, null);
    }

    public static PaymentApprovalResult fail(UUID orderId, String failureCode, String failureMessage) {
        return new PaymentApprovalResult(orderId, false, null, null, failureCode, failureMessage);
    }
}

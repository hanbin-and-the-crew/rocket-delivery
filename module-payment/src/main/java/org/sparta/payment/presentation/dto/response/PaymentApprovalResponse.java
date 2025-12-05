package org.sparta.payment.presentation.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import org.sparta.payment.application.dto.PaymentApprovalResult;

/**
 * PaymentApproval API 응답 DTO
 */
public record PaymentApprovalResponse(
        UUID orderId,
        boolean approved,
        String paymentKey,
        LocalDateTime approvedAt,
        String failureCode,
        String failureMessage
) {

    public static PaymentApprovalResponse from(PaymentApprovalResult result) {
        return new PaymentApprovalResponse(
                result.orderId(),
                result.approved(),
                result.paymentKey(),
                result.approvedAt(),
                result.failureCode(),
                result.failureMessage()
        );
    }
}

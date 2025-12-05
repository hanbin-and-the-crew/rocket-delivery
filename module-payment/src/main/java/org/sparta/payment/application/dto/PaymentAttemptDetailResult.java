package org.sparta.payment.application.dto;

import org.sparta.payment.domain.entity.PaymentAttempt;
import org.sparta.payment.domain.enumeration.PaymentAttemptStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentAttemptDetailResult(
        UUID paymentAttemptId,
        UUID paymentId,
        Integer attemptNo,
        String pgTransactionId,
        PaymentAttemptStatus status,
        String requestPayload,
        String responsePayload,
        String errorCode,
        String errorMessage,
        LocalDateTime requestedAt,
        LocalDateTime respondedAt,
        LocalDateTime createdAt
) {

    public static PaymentAttemptDetailResult from(PaymentAttempt attempt) {
        return new PaymentAttemptDetailResult(
                attempt.getPaymentAttemptId(),
                attempt.getPaymentId(),
                attempt.getAttemptNo(),
                attempt.getPgTransactionId(),
                attempt.getStatus(),
                attempt.getRequestPayload(),
                attempt.getResponsePayload(),
                attempt.getErrorCode(),
                attempt.getErrorMessage(),
                attempt.getRequestedAt(),
                attempt.getRespondedAt(),
                attempt.getCreatedAt()
        );
    }
}

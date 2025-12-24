package org.sparta.payment.application.dto;

import org.sparta.payment.domain.entity.Refund;
import org.sparta.payment.domain.enumeration.RefundStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record RefundDetailResult(
        UUID refundId,
        UUID paymentId,
        String refundKey,
        Long amount,
        RefundStatus status,
        String reason,
        LocalDateTime requestedAt,
        LocalDateTime completedAt,
        LocalDateTime failedAt,
        String failureCode,
        String failureMessage,
        LocalDateTime createdAt
) {

    public static RefundDetailResult from(Refund refund) {
        return new RefundDetailResult(
                refund.getRefundId(),
                refund.getPaymentId(),
                refund.getRefundKey(),
                refund.getAmount(),
                refund.getStatus(),
                refund.getReason(),
                refund.getRequestedAt(),
                refund.getCompletedAt(),
                refund.getFailedAt(),
                refund.getFailureCode(),
                refund.getFailureMessage(),
                refund.getCreatedAt()
        );
    }
}

package org.sparta.payment.presentation.dto.response;

import org.sparta.payment.application.dto.RefundDetailResult;
import org.sparta.payment.domain.enumeration.RefundStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record RefundDetailResponse(
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

    public static RefundDetailResponse from(RefundDetailResult result) {
        return new RefundDetailResponse(
                result.refundId(),
                result.paymentId(),
                result.refundKey(),
                result.amount(),
                result.status(),
                result.reason(),
                result.requestedAt(),
                result.completedAt(),
                result.failedAt(),
                result.failureCode(),
                result.failureMessage(),
                result.createdAt()
        );
    }
}

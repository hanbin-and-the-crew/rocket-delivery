package org.sparta.payment.presentation.dto.response;

import org.sparta.payment.application.dto.PaymentDetailResult;
import org.sparta.payment.domain.enumeration.PaymentStatus;
import org.sparta.payment.domain.enumeration.PaymentType;
import org.sparta.payment.domain.enumeration.PgProvider;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentDetailResponse(
        UUID paymentId,
        UUID orderId,
        String paymentKey,
        Long amountTotal,
        Long amountCoupon,
        Long amountPoint,
        Long amountPayable,
        Long amountPaid,
        String currency,
        PaymentStatus status,
        PaymentType methodType,
        PgProvider pgProvider,
        String failureCode,
        String failureMessage,
        LocalDateTime requestedAt,
        LocalDateTime approvedAt,
        LocalDateTime canceledAt,
        UUID couponId,
        UUID pointUsageId
) {
    public static PaymentDetailResponse from(PaymentDetailResult result) {
        return new PaymentDetailResponse(
                result.paymentId(),
                result.orderId(),
                result.paymentKey(),
                result.amountTotal(),
                result.amountCoupon(),
                result.amountPoint(),
                result.amountPayable(),
                result.amountPaid(),
                result.currency(),
                result.status(),
                result.methodType(),
                result.pgProvider(),
                result.failureCode(),
                result.failureMessage(),
                result.requestedAt(),
                result.approvedAt(),
                result.canceledAt(),
                result.couponId(),
                result.pointUsageId()
        );
    }
}

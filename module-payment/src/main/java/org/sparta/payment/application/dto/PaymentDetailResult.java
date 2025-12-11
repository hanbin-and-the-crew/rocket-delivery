package org.sparta.payment.application.dto;

import org.sparta.payment.domain.entity.Payment;
import org.sparta.payment.domain.enumeration.PaymentStatus;
import org.sparta.common.domain.PaymentType;
import org.sparta.common.domain.PgProvider;

import java.time.LocalDateTime;
import java.util.UUID;


public record PaymentDetailResult(
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

    public static PaymentDetailResult from(Payment payment) {
        return new PaymentDetailResult(
                payment.getPaymentId(),
                payment.getOrderId(),
                payment.getPaymentKey(),
                payment.getAmountTotal(),
                payment.getAmountCoupon(),
                payment.getAmountPoint(),
                payment.getAmountPayable(),
                payment.getAmountPaid(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getMethodType(),
                payment.getPgProvider(),
                payment.getFailureCode(),
                payment.getFailureMessage(),
                payment.getRequestedAt(),
                payment.getApprovedAt(),
                payment.getCanceledAt(),
                payment.getCouponId(),
                payment.getPointUsageId()
        );
    }
}

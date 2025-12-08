package org.sparta.payment.application.dto;

import org.sparta.payment.domain.entity.Payment;
import org.sparta.payment.domain.enumeration.PaymentStatus;
import org.sparta.payment.domain.enumeration.PaymentType;
import org.sparta.payment.domain.enumeration.PgProvider;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentSummaryResult(
        UUID paymentId,
        UUID orderId,
        Long amountTotal,
        Long amountPaid,
        PaymentStatus status,
        PaymentType methodType,
        PgProvider pgProvider,
        LocalDateTime requestedAt
) {

    public static PaymentSummaryResult from(Payment payment) {
        return new PaymentSummaryResult(
                payment.getPaymentId(),
                payment.getOrderId(),
                payment.getAmountTotal(),
                payment.getAmountPaid(),
                payment.getStatus(),
                payment.getMethodType(),
                payment.getPgProvider(),
                payment.getRequestedAt()
        );
    }
}

package org.sparta.payment.presentation.dto.response;

import org.sparta.payment.application.dto.PaymentSummaryResult;
import org.sparta.payment.domain.enumeration.PaymentStatus;
import org.sparta.payment.domain.enumeration.PaymentType;
import org.sparta.payment.domain.enumeration.PgProvider;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentSummaryResponse(
        UUID paymentId,
        UUID orderId,
        Long amountTotal,
        Long amountPaid,
        PaymentStatus status,
        PaymentType methodType,
        PgProvider pgProvider,
        LocalDateTime requestedAt
) {

    public static PaymentSummaryResponse from(PaymentSummaryResult result) {
        return new PaymentSummaryResponse(
                result.paymentId(),
                result.orderId(),
                result.amountTotal(),
                result.amountPaid(),
                result.status(),
                result.methodType(),
                result.pgProvider(),
                result.requestedAt()
        );
    }
}

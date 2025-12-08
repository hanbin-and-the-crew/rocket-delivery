package org.sparta.payment.presentation.dto.request;

public record PaymentRefundPartialRequest(
        Long refundAmount,
        String reason
) {}

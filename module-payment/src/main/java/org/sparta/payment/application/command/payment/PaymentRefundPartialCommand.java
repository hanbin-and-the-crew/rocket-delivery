package org.sparta.payment.application.command.payment;

import java.util.UUID;

public record PaymentRefundPartialCommand(
        UUID paymentId,
        Long refundAmount,
        String reason
) {}

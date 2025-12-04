package org.sparta.payment.application.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentRefundedPayload(
        UUID paymentId,
        UUID orderId,
        Long refundedAmount,
        LocalDateTime refundedAt
) {}

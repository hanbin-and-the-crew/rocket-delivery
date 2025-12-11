package org.sparta.common.event.payment;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentRefundedEvent(
        UUID paymentId,
        UUID orderId,
        Long refundedAmount,
        LocalDateTime refundedAt
) {}

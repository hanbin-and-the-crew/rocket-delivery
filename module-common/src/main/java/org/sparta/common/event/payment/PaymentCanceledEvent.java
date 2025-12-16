package org.sparta.common.event.payment;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentCanceledEvent(
        UUID paymentId,
        UUID orderId,
        Long amount,
        LocalDateTime canceledAt
) {}

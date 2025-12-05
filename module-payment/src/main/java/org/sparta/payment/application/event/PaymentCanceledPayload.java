package org.sparta.payment.application.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentCanceledPayload(
        UUID paymentId,
        UUID orderId,
        Long amount,
        LocalDateTime canceledAt
) {}

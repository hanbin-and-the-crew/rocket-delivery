package org.sparta.payment.application.event;

import java.util.UUID;

public record PaymentFailedPayload(
        UUID paymentId,
        UUID orderId,
        Long amount,
        String currency,
        String failureCode,
        String failureMessage
) {}

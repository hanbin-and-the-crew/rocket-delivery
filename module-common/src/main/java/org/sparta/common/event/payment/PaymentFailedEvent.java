package org.sparta.common.event.payment;

import java.util.UUID;

public record PaymentFailedEvent(
        UUID paymentId,
        UUID orderId,
        Long amount,
        String currency,
        String failureCode,
        String failureMessage
) {}

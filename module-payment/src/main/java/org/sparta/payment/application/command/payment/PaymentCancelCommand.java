package org.sparta.payment.application.command.payment;

import java.util.UUID;

public record PaymentCancelCommand(
        UUID paymentId,
        String reason
) {}

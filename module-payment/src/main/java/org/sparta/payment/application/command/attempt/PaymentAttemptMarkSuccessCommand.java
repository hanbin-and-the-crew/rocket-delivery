package org.sparta.payment.application.command.attempt;

import java.util.UUID;

public record PaymentAttemptMarkSuccessCommand(
        UUID paymentAttemptId,
        String pgTransactionId,
        String responsePayload
) {}

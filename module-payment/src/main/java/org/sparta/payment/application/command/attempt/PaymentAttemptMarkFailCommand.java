package org.sparta.payment.application.command.attempt;

import java.util.UUID;

public record PaymentAttemptMarkFailCommand(
        UUID paymentAttemptId,
        String errorCode,
        String errorMessage,
        String responsePayload
) {}

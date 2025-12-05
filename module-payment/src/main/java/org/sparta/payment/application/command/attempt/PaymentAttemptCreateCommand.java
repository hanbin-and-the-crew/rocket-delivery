package org.sparta.payment.application.command.attempt;

import java.util.UUID;

public record PaymentAttemptCreateCommand(
        UUID paymentId,
        Integer attemptNo,
        String requestPayload
) {}

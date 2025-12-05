package org.sparta.payment.application.command.attempt;

import java.util.UUID;

public record PaymentAttemptGetByPaymentIdCommand(
        UUID paymentId
) {}

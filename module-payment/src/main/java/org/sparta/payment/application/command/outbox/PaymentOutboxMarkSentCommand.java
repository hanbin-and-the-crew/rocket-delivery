package org.sparta.payment.application.command.outbox;

import java.util.UUID;

public record PaymentOutboxMarkSentCommand(
        UUID paymentOutboxId
) {}

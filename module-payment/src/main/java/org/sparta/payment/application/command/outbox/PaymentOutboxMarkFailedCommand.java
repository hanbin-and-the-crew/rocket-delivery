package org.sparta.payment.application.command.outbox;

import java.util.UUID;

public record PaymentOutboxMarkFailedCommand(
        UUID paymentOutboxId
) {}

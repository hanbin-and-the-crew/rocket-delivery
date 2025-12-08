package org.sparta.payment.application.command.outbox;

import java.util.UUID;

public record PaymentOutboxCreateCommand(
        String aggregateType,
        UUID aggregateId,
        String eventType,
        String payload
) {}

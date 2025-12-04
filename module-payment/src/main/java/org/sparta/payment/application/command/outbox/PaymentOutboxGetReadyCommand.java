package org.sparta.payment.application.command.outbox;

public record PaymentOutboxGetReadyCommand(
        int limit
) {}

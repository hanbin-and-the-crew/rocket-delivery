package org.sparta.payment.application.command.outbox;

import jakarta.validation.constraints.Min;

public record PaymentOutboxGetReadyCommand(
        @Min(value = 1, message = "limit은 1 이상이어야 합니다.")
        int limit
) {}

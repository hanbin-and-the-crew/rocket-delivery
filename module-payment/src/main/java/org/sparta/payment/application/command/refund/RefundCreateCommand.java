package org.sparta.payment.application.command.refund;

import java.util.UUID;

public record RefundCreateCommand(
        UUID paymentId,
        Long amount,
        String reason
) {}

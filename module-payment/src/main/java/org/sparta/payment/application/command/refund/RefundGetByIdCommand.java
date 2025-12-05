package org.sparta.payment.application.command.refund;

import java.util.UUID;

public record RefundGetByIdCommand(
        UUID refundId
) {}

package org.sparta.payment.application.command.refund;

import java.util.UUID;

public record RefundGetByPaymentIdCommand(
        UUID paymentId
) {}

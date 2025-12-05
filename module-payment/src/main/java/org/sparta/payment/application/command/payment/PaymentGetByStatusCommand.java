package org.sparta.payment.application.command.payment;

import org.sparta.payment.domain.enumeration.PaymentStatus;

public record PaymentGetByStatusCommand(
        PaymentStatus status
) {}

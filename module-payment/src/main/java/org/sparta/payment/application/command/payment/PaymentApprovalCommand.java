package org.sparta.payment.application.command.payment;

import org.sparta.common.domain.PaymentType;
import org.sparta.common.domain.PgProvider;

import java.util.UUID;

public record PaymentApprovalCommand(
        UUID orderId,
        String pgToken,
        Long amountPayable,
        PaymentType methodType,
        PgProvider pgProvider,
        String currency
) {}

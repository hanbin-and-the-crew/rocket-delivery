package org.sparta.payment.application.command.payment;

import org.sparta.common.domain.PaymentType;
import org.sparta.common.domain.PgProvider;

import java.util.UUID;

/**
 * PaymentService에서 사용하는 생성용 Command
 */
public record PaymentCreateCommand(
        UUID orderId,
        Long amountTotal,
        Long amountCoupon,
        Long amountPoint,
        Long amountPayable,
        PaymentType methodType,
        PgProvider pgProvider,
        String currency,
        UUID couponId,
        UUID pointUsageId
) {}

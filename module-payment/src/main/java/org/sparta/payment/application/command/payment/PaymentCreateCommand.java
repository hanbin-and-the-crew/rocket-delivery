package org.sparta.payment.application.command.payment;

import java.util.UUID;
import org.sparta.payment.domain.enumeration.PaymentType;
import org.sparta.payment.domain.enumeration.PgProvider;

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

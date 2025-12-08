package org.sparta.payment.application.dto;

import org.sparta.payment.domain.entity.Payment;

import java.util.List;

public record PaymentListResult(
        List<PaymentSummaryResult> payments
) {

    public static PaymentListResult from(List<Payment> list) {
        return new PaymentListResult(
                list.stream()
                        .map(PaymentSummaryResult::from)
                        .toList()
        );
    }
}

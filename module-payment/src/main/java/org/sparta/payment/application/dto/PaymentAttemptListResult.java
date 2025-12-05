package org.sparta.payment.application.dto;

import org.sparta.payment.domain.entity.PaymentAttempt;

import java.util.List;

public record PaymentAttemptListResult(
        List<PaymentAttemptDetailResult> attempts
) {

    public static PaymentAttemptListResult from(List<PaymentAttempt> list) {
        return new PaymentAttemptListResult(
                list.stream()
                        .map(PaymentAttemptDetailResult::from)
                        .toList()
        );
    }
}

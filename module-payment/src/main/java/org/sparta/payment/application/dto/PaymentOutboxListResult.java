package org.sparta.payment.application.dto;

import org.sparta.payment.domain.entity.PaymentOutbox;

import java.util.List;

public record PaymentOutboxListResult(
        List<PaymentOutboxDetailResult> outboxes
) {

    public static PaymentOutboxListResult from(List<PaymentOutbox> list) {
        return new PaymentOutboxListResult(
                list.stream()
                        .map(PaymentOutboxDetailResult::from)
                        .toList()
        );
    }
}

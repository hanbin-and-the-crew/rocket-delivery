package org.sparta.payment.application.dto;

import org.sparta.payment.domain.entity.Refund;

import java.util.List;

public record RefundListResult(
        List<RefundDetailResult> refunds
) {

    public static RefundListResult from(List<Refund> list) {
        return new RefundListResult(
                list.stream()
                        .map(RefundDetailResult::from)
                        .toList()
        );
    }
}

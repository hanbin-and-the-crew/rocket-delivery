package org.sparta.payment.presentation.dto.response;

import org.sparta.payment.application.dto.PaymentListResult;

import java.util.List;

public record PaymentListResponse(
        List<PaymentSummaryResponse> payments
) {

    public static PaymentListResponse from(PaymentListResult result) {
        return new PaymentListResponse(
                result.payments().stream()
                        .map(PaymentSummaryResponse::from)
                        .toList()
        );
    }
}

package org.sparta.payment.presentation.dto.response;

import org.sparta.payment.application.dto.RefundListResult;

import java.util.List;

public record RefundListResponse(
        List<RefundDetailResponse> refunds
) {

    public static RefundListResponse from(RefundListResult result) {
        return new RefundListResponse(
                result.refunds().stream()
                        .map(RefundDetailResponse::from)
                        .toList()
        );
    }
}

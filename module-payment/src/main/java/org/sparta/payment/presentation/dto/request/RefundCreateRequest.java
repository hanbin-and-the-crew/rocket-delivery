package org.sparta.payment.presentation.dto.request;

import java.util.UUID;

public record RefundCreateRequest(
        UUID paymentId,
        Long amount,
        String reason
) {}

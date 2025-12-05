package org.sparta.payment.presentation.dto.request;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RefundCreateRequest(
        @NotNull(message = "결제 ID는 필수입니다")
        UUID paymentId,
        @NotNull(message = "환불 금액은 필수입니다")
        @Positive(message = "환불 금액은 양수여야 합니다")
        Long amount,
        @NotBlank(message = "환불 사유는 필수입니다")
        String reason
) {}

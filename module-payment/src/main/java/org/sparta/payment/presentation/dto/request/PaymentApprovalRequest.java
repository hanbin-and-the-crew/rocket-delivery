package org.sparta.payment.presentation.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.sparta.payment.domain.enumeration.PaymentType;
import org.sparta.payment.domain.enumeration.PgProvider;

import java.util.UUID;

/**
 * Order 서비스가 결제 승인(PG mock)을 요청할 때 사용하는 DTO
 * - 클라이언트가 PG 결제창을 통해 얻은 pgToken을 전달받는다.
 */
public record PaymentApprovalRequest(

        @NotNull(message = "orderId는 필수입니다.")
        UUID orderId,

        @NotBlank(message = "pgToken은 필수입니다.")
        String pgToken,

        @NotNull(message = "amountPayable은 필수입니다.")
        @Min(value = 0, message = "amountPayable은 0 이상이어야 합니다.")
        Long amountPayable,

        @NotNull(message = "결제 수단(methodType)은 필수입니다.")
        PaymentType methodType,

        @NotNull(message = "PG사(pgProvider)는 필수입니다.")
        PgProvider pgProvider,

        @NotBlank(message = "currency는 필수입니다.")
        String currency
) {}

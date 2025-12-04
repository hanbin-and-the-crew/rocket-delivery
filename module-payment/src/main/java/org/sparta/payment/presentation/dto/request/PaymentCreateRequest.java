package org.sparta.payment.presentation.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.sparta.payment.domain.enumeration.PaymentType;
import org.sparta.payment.domain.enumeration.PgProvider;

/**
 * 클라이언트(또는 Order 서버)가 Payment 생성 요청 시 사용하는 DTO
 * - amountTotal, 쿠폰/포인트, amountPayable 까지 모두 포함
 */
public record PaymentCreateRequest(

        @NotNull(message = "orderId는 필수입니다.")
        UUID orderId,

        @NotNull(message = "amountTotal은 필수입니다.")
        @Min(value = 1, message = "amountTotal은 1 이상이어야 합니다.")
        Long amountTotal,

        @NotNull(message = "amountCoupon은 필수입니다.")
        @Min(value = 0, message = "amountCoupon은 0 이상이어야 합니다.")
        Long amountCoupon,

        @NotNull(message = "amountPoint은 필수입니다.")
        @Min(value = 0, message = "amountPoint은 0 이상이어야 합니다.")
        Long amountPoint,

        @NotNull(message = "amountPayable은 필수입니다.")
        @Min(value = 0, message = "amountPayable은 0 이상이어야 합니다.")
        Long amountPayable,

        @NotNull(message = "결제 수단(methodType)은 필수입니다.")
        PaymentType methodType,

        @NotNull(message = "PG사(pgProvider)는 필수입니다.")
        PgProvider pgProvider,

        @NotBlank(message = "currency는 필수입니다.")
        String currency,

        UUID couponId,
        UUID pointUsageId
) {}

package org.sparta.product.presentation.dto.stock;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/**
 * 재고 예약/확정/취소 요청 DTO 모음
 */
public class StockRequest {

    @Schema(description = "재고 예약 요청")
    public record Reserve(
            @Schema(description = "상품 ID", example = "b8e8b6f5-1234-4c6c-9b8e-1a2b3c4d5e6f")
            @NotNull(message = "productId는 필수입니다.")
            UUID productId,

            @Schema(description = "예약 키 (orderItemId 등)", example = "orderItem-20251203-0001")
            @NotBlank(message = "reservationKey는 필수입니다.")
            String reservationKey,

            @Schema(description = "예약 수량", example = "3")
            @Positive(message = "quantity는 1 이상이어야 합니다.")
            int quantity
    ) {
    }

    @Schema(description = "재고 예약 확정 요청 (결제 성공 시)")
    public record Confirm(
            @Schema(description = "예약 키 (orderItemId 등)", example = "orderItem-20251203-0001")
            @NotBlank(message = "reservationKey는 필수입니다.")
            String reservationKey
    ) {
    }

    @Schema(description = "재고 예약 취소 요청 (주문 취소 / 결제 실패 시)")
    public record Cancel(
            @Schema(description = "예약 키 (orderItemId 등)", example = "orderItem-20251203-0001")
            @NotBlank(message = "reservationKey는 필수입니다.")
            String reservationKey
    ) {
    }
}

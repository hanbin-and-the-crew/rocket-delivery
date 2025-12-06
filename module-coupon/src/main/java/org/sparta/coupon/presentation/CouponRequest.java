package org.sparta.coupon.presentation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public class CouponRequest {

    @Schema(description = "쿠폰 검증 및 예약 요청")
    public record Reserve(
            @Schema(description = "사용자 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @NotNull(message = "사용자 ID는 필수입니다")
            UUID userId,

            @Schema(description = "주문 ID", example = "550e8400-e29b-41d4-a716-446655440001")
            @NotNull(message = "주문 ID는 필수입니다")
            UUID orderId,

            @Schema(description = "주문 금액", example = "50000")
            @NotNull(message = "주문 금액은 필수입니다")
            @Positive(message = "주문 금액은 0보다 커야 합니다")
            Long orderAmount
    ) {}

    @Schema(description = "쿠폰 사용 확정 요청")
    public record Confirm(
            @Schema(description = "예약 ID", example = "550e8400-e29b-41d4-a716-446655440002")
            @NotNull(message = "예약 ID는 필수입니다")
            UUID reservationId,

            @Schema(description = "주문 ID", example = "550e8400-e29b-41d4-a716-446655440001")
            @NotNull(message = "주문 ID는 필수입니다")
            UUID orderId
    ) {}

    @Schema(description = "쿠폰 예약 취소 요청")
    public record Cancel(
            @Schema(description = "예약 ID", example = "550e8400-e29b-41d4-a716-446655440002")
            @NotNull(message = "예약 ID는 필수입니다")
            UUID reservationId,

            @Schema(description = "취소 사유", example = "ORDER_FAILED")
            @NotBlank(message = "취소 사유는 필수입니다")
            String reason
    ) {}
}
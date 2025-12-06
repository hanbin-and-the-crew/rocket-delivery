package org.sparta.user.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public class PointRequest {

    @Schema(description = "결제시 포인트 요청(예약)")
    public record Reserve(
            @Schema(description = "사용자 ID", example = "61a98cf7-921c-47fb-a802-3ec71f736f74")
            @NotNull(message = "사용자 ID는 필수입니다")
            UUID userId,

            @Schema(description = "주문 ID", example = "61a98cf7-921c-47fb-a802-3ec71f736f75")
            @NotNull(message = "주문 ID는 필수입니다")
            UUID orderId,

            @Schema(description = "결제 가격", example = "150000")
            @NotNull(message = "가격은 필수입니다")
            @Positive(message = "가격은 0보다 커야 합니다")
            Long orderAmount,

            @Schema(description = "포인트", example = "3000")
            @NotNull(message = "요청 포인트는 필수입니다")
            @Positive(message = "요청 포인트는 0보다 커야 합니다")
            Long requestPoint
    ) {}

    @Schema(description = "결제 완료 이후 포인트 확정")
    public record Confirm(
            @Schema(description = "주문 ID", example = "61a98cf7-921c-47fb-a802-3ec71f736f75")
            @NotNull(message = "주문 ID는 필수입니다")
            UUID orderId
    ) {}
}

package org.sparta.order.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class PaymentRequest {
    @Schema(description = "결제 생성 요청")
    public record Create(
            @Schema(description = "주문 ID", example = "550e8400-e29b-41d4-a716-446655440004")
            @NotNull(message = "주문 ID는 필수입니다")
            UUID orderId,

            @Schema(description = "상품 ID", example = "550e8400-e29b-41d4-a716-446655440005")
            @NotNull(message = "상품 ID는 필수입니다")
            UUID productId,

            @Schema(description = "주문 수량", example = "10")
            @NotNull(message = "주문 수량은 필수입니다")
            @Min(value = 1, message = "주문 수량은 최소 1개 이상이어야 합니다")
            Integer quantity
    ) {
    }
}

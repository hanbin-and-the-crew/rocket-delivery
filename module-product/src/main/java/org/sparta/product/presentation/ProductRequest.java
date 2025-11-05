package org.sparta.product.presentation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class ProductRequest {

    @Schema(description = "상품 생성 요청")
    public record Create(
            @Schema(description = "상품명", example = "노트북")
            @NotBlank(message = "상품명은 필수입니다")
            String name,

            @Schema(description = "상품 설명", example = "고성능 노트북")
            String description,

            @Schema(description = "가격", example = "1500000")
            @NotNull(message = "가격은 필수입니다")
            @Positive(message = "가격은 0보다 커야 합니다")
            Integer price,

            @Schema(description = "재고 수량", example = "100")
            @NotNull(message = "재고 수량은 필수입니다")
            @Positive(message = "재고는 0보다 커야 합니다")
            Integer stock
    ) {
    }
}
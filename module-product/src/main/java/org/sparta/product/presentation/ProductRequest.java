package org.sparta.product.presentation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public class ProductRequest {

    @Schema(description = "상품 생성 요청")
    public record Create(
            @Schema(description = "상품명", example = "노트북")
            @NotBlank(message = "상품명은 필수입니다")
            String name,

            @Schema(description = "가격", example = "1500000")
            @NotNull(message = "가격은 필수입니다")
            @Positive(message = "가격은 0보다 커야 합니다")
            Long price,

            @Schema(description = "카테고리 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @NotNull(message = "카테고리 ID는 필수입니다")
            UUID categoryId,

            @Schema(description = "회사 ID", example = "550e8400-e29b-41d4-a716-446655440001")
            @NotNull(message = "회사 ID는 필수입니다")
            UUID companyId,

            @Schema(description = "허브 ID", example = "550e8400-e29b-41d4-a716-446655440002")
            @NotNull(message = "허브 ID는 필수입니다")
            UUID hubId,

            @Schema(description = "재고 수량", example = "100")
            @NotNull(message = "재고 수량은 필수입니다")
            @Positive(message = "재고는 0보다 커야 합니다")
            Integer stock
    ) {
    }

    @Schema(description = "상품 수정 요청")
    public record Update(
            @Schema(description = "상품명", example = "수정된 노트북")
            String name,

            @Schema(description = "가격", example = "2000000")
            @Positive(message = "가격은 0보다 커야 합니다")
            Long price
    ) {
    }
}
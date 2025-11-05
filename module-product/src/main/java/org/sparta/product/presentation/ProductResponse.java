package org.sparta.product.presentation;

import io.swagger.v3.oas.annotations.media.Schema;

public class ProductResponse {

    @Schema(description = "상품 생성 응답")
    public record Create(
            @Schema(description = "상품 ID", example = "1")
            Long id,

            @Schema(description = "상품명", example = "노트북")
            String name,

            @Schema(description = "상품 설명", example = "고성능 노트북")
            String description,

            @Schema(description = "가격", example = "1500000")
            Integer price,

            @Schema(description = "재고 수량", example = "100")
            Integer stock
    ) {
    }
}

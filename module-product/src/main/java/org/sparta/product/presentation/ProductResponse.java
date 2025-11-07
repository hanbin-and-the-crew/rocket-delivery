package org.sparta.product.presentation;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sparta.product.domain.entity.Product;
import org.sparta.product.domain.vo.Money;

import java.util.UUID;

public class ProductResponse {

    @Schema(description = "상품 생성 응답")
    public record Create(
            @Schema(description = "상품 ID", example = "1")
            UUID id,

            @Schema(description = "상품명", example = "노트북")
            String name,

            @Schema(description = "가격", example = "1500000")
            Long price

    ) {
        public static Create of(Product product){
            return new Create(
              product.getId(),
              product.getProductName(),
              product.getPrice().getAmount()
            );
        }
    }
}

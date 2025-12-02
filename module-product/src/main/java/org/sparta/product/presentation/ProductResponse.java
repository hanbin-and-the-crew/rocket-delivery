package org.sparta.product.presentation;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sparta.product.domain.entity.Product;
import org.sparta.product.domain.entity.Stock;

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

    @Schema(description = "상품 상세 조회 응답")
    public record Detail(
            @Schema(description = "상품 ID")
            UUID productId,

            @Schema(description = "상품명")
            String name,

            @Schema(description = "가격")
            Long price,

            @Schema(description = "재고 수량")
            Integer quantity
    ) {
        public static Detail of(Product product, Stock stock) {
            return new Detail(
                    product.getId(),
                    product.getProductName(),
                    product.getPrice().getAmount(),
                    stock.getQuantity()
            );
        }
    }

    @Schema(description = "상품 수정 응답")
    public record Update(
            @Schema(description = "상품 ID")
            UUID productId,

            @Schema(description = "수정된 상품명")
            String name,

            @Schema(description = "수정된 가격")
            Long price
    ) {
        public static Update of(Product product) {
            return new Update(
                    product.getId(),
                    product.getProductName(),
                    product.getPrice().getAmount()
            );
        }
    }
}

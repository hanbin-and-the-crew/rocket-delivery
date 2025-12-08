package org.sparta.product.presentation.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sparta.product.application.dto.ProductDetailInfo;

import java.util.UUID;

/**
 * Product API 응답 DTO
 *
 * - presentation 계층에서만 사용하는 응답 모델
 * - 도메인 엔티티(Product, Stock) 대신 application 계층의 ProductDetailInfo에만 의존
 */
public class ProductResponse {

    @Schema(description = "상품 생성 응답")
    public record Create(
            @Schema(description = "상품 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            UUID id,

            @Schema(description = "상품명", example = "노트북")
            String name,

            @Schema(description = "가격", example = "1500000")
            Long price
    ) {
        /**
         * ProductDetailInfo → Create 응답으로 매핑
         */
        public static Create of(ProductDetailInfo detail) {
            return new Create(
                    detail.productId(),
                    detail.productName(),
                    detail.price()
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
            // 필요하면 여기서 reservedQuantity, isActive 등도 추가 가능
    ) {
        /**
         * ProductDetailInfo → Detail 응답으로 매핑
         */
        public static Detail of(ProductDetailInfo detail) {
            return new Detail(
                    detail.productId(),
                    detail.productName(),
                    detail.price(),
                    detail.quantity()
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
        /**
         * ProductDetailInfo → Update 응답으로 매핑
         */
        public static Update of(ProductDetailInfo detail) {
            return new Update(
                    detail.productId(),
                    detail.productName(),
                    detail.price()
            );
        }
    }
}

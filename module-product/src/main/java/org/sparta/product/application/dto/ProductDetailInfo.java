package org.sparta.product.application.dto;

import org.sparta.product.domain.entity.Product;
import org.sparta.product.domain.entity.Stock;

import java.util.UUID;

/**
 * 상품 + 재고 통합 조회 결과
 */
public record ProductDetailInfo(
        UUID productId,
        String productName,
        long price,
        UUID categoryId,
        UUID companyId,
        UUID hubId,
        int quantity,
        int reservedQuantity,
        boolean isActive
) {

    public static ProductDetailInfo of(Product product, Stock stock) {
        return new ProductDetailInfo(
                product.getId(),
                product.getProductName(),
                product.getPrice().getAmount(),
                product.getCategoryId(),
                product.getCompanyId(),
                product.getHubId(),
                stock.getQuantity(),
                stock.getReservedQuantity(),
                Boolean.TRUE.equals(product.getIsActive())
        );
    }
}

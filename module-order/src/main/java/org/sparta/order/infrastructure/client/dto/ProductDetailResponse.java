package org.sparta.order.infrastructure.client.dto;

import java.util.UUID;

/**
 * Product 서비스로부터 받는 상품 상세 정보
 */
public record ProductDetailResponse(
        UUID productId,
        String name,
        Long price,
        Integer quantity  // 재고 수량
) {
}
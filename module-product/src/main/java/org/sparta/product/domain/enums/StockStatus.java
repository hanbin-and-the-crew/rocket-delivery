package org.sparta.product.domain.enums;

/**
 * 재고 상태
 * - IN_STOCK: 판매 가능 (가용 재고 있음)
 * - OUT_OF_STOCK: 재고 소진 (총 재고 0)
 * - RESERVED_ONLY: 모두 예약됨 (실물은 있지만 가용 재고 0)
 * - UNAVAILABLE: 판매 불가 (상품 삭제됨)
 */
public enum StockStatus {
    IN_STOCK,
    OUT_OF_STOCK,
    RESERVED_ONLY,
    UNAVAILABLE
}

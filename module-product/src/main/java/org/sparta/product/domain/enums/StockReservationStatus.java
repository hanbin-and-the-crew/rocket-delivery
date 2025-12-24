package org.sparta.product.domain.enums;

/**
 * - RESERVED  : 재고가 확보되어 예약된 상태
 * - CONFIRMED : 결제 성공으로 실제 재고 차감이 확정된 상태
 * - CANCELLED : 주문 취소 / 결제 실패 등으로 예약이 취소된 상태
 */
public enum StockReservationStatus {
    RESERVED,
    CONFIRMED,
    CANCELLED
}

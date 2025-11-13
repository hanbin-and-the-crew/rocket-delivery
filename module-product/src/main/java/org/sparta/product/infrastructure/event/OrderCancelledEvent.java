package org.sparta.product.infrastructure.event;

import java.time.Instant;
import java.util.UUID;

/**
 * 주문 취소 이벤트 (Order 모듈에서 발행) // 수신용 DTO
 *
 * Product 모듈 관점:
 * - 이 이벤트를 수신하면 예약된 재고를 취소한다
 * - 취소 성공 시: StockReservationCancelledEvent 발행
 */
public record OrderCancelledEvent(
        UUID eventId,
        UUID orderId,
        UUID productId,
        Integer quantity,
        Instant occurredAt
) {
}
package org.sparta.product.infrastructure.event.kafka.dto;

import org.sparta.common.event.DomainEvent; // ✅ 공통 이벤트 인터페이스 import
import java.time.Instant;
import java.util.UUID;

/**
 * 재고 예약 실패 이벤트 (Product 모듈에서 발행)
 *
 * Order 모듈로 전달:
 * - 재고 예약이 실패했음을 알림
 * - Order 모듈에서 주문을 취소 처리
 */
public record StockReservationFailedEvent(
        UUID eventId,
        UUID orderId,
        UUID productId,
        Integer requestedQuantity,
        String reason,  // 실패 사유 (ex: "재고 부족", "상품 없음")
        Instant occurredAt
) implements DomainEvent {

    public static StockReservationFailedEvent of(UUID orderId, UUID productId, Integer requested, String reason) {
        return new StockReservationFailedEvent(
                UUID.randomUUID(),
                orderId,
                productId,
                requested,
                reason,
                Instant.now()
        );
    }
}

package org.sparta.product.infrastructure.event.publisher;

import org.sparta.common.event.DomainEvent; // ✅ 공통 이벤트 인터페이스 import
import java.time.Instant;
import java.util.UUID;

/**
 * 재고 예약 취소 완료 이벤트 (Product 모듈에서 발행)
 *
 * Order 모듈로 전달:
 * - 예약된 재고가 취소되었음을 알림
 * - Order 모듈에서 주문 상태를 'CANCELLED'로 최종 확정
 */
public record StockReservationCancelledEvent(
        UUID eventId,
        UUID orderId,
        UUID productId,
        Integer cancelledQuantity,
        Instant occurredAt
) implements DomainEvent {

    public static StockReservationCancelledEvent of(UUID orderId, UUID productId, Integer cancelled) {
        return new StockReservationCancelledEvent(
                UUID.randomUUID(),
                orderId,
                productId,
                cancelled,
                Instant.now()
        );
    }
}

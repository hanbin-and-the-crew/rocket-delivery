package org.sparta.product.infrastructure.event.kafka.dto;

import org.sparta.common.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * 재고 확정 완료 이벤트 (Product 모듈 발행)
 *
 * Order/Delivery 모듈로 전달:
 * - 예약된 재고가 실제로 차감되었음을 알림
 * - Order 모듈에서 주문 상태를 'CONFIRMED'로 변경
 * - Delivery 모듈에서 배송 준비 시작
 */
public record StockConfirmedEvent(
        UUID eventId,
        UUID orderId,
        UUID productId,
        Integer confirmedQuantity,
        Integer remainingQuantity,  // 확정 후 남은 실제 재고
        Instant occurredAt
) implements DomainEvent {
    public static StockConfirmedEvent of(UUID orderId, UUID productId, Integer confirmed, Integer remaining) {
        return new StockConfirmedEvent(
                UUID.randomUUID(),
                orderId,
                productId,
                confirmed,
                remaining,
                Instant.now()
        );
    }
}

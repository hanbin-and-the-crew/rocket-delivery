package org.sparta.product.infrastructure.event.publisher;

import org.sparta.common.event.DomainEvent;
import org.sparta.product.domain.enums.StockStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * 재고 예약 성공 이벤트 (Product 모듈에서 발행)
 *
 * Order 모듈로 전달:
 * - 재고 예약이 성공했음을 알림
 * - Order 모듈에서 주문 상태를 'STOCK_RESERVED'로 변경
 */
public record StockReservedEvent(
        UUID eventId,
        UUID orderId,
        UUID productId,
        Integer reservedQuantity,
        Integer availableQuantity,  // 예약 후 남은 가용 재고
        StockStatus status,
        Instant occurredAt
) implements DomainEvent {

    public static StockReservedEvent of(UUID orderId, UUID productId, Integer reserved, Integer available, StockStatus status) {
        return new StockReservedEvent(
                UUID.randomUUID(),
                orderId,
                productId,
                reserved,
                available,
                status,
                Instant.now()
        );
    }
}

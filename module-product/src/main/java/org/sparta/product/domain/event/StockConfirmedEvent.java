package org.sparta.product.domain.event;

import org.sparta.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * 재고 예약 확정(실차감) 완료 이벤트
 *
 * - OrderCreatedEvent(주문 완료 이벤트)를 수신한 뒤
 *   해당 주문에 대한 재고 예약을 확정(실차감)하면 발행된다.
 * - 다른 모듈(예: Order, 통계, 모니터링 등)에서
 *   "이 주문에 대한 재고 차감이 실제로 완료됐다"는 사실을 구독할 수 있다.
 */
public record StockConfirmedEvent(
        UUID eventId,
        UUID orderId,
        UUID productId,
        Integer confirmedQuantity,
        Instant occurredAt
) implements DomainEvent {

    /**
     * 이벤트 생성 팩토리 메서드
     */
    public static StockConfirmedEvent of(
            UUID orderId,
            UUID productId,
            Integer confirmedQuantity
    ) {
        return new StockConfirmedEvent(
                UUID.randomUUID(),   // 이벤트 ID (멱등/추적용)
                orderId,
                productId,
                confirmedQuantity,
                Instant.now()
        );
    }
}

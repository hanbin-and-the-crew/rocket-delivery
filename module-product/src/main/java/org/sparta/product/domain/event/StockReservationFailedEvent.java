package org.sparta.product.domain.event;

import org.sparta.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * 재고 예약 확정/차감 실패 이벤트
 * - OrderCreatedEvent 기반 재고 확정 처리 중 오류가 발생했을 때 발행된다.
 * - 다른 모듈/모니터링에서 "이 주문의 재고 처리에 실패했다"는 사실을 구독할 수 있다.
 */
public record StockReservationFailedEvent(
        UUID eventId,
        UUID orderId,
        String reservationKey,
        String errorCode,
        String errorMessage,
        Instant occurredAt
) implements DomainEvent {

    public static StockReservationFailedEvent of(
            UUID orderId,
            String reservationKey,
            String errorCode,
            String errorMessage
    ) {
        return new StockReservationFailedEvent(
                UUID.randomUUID(),   // 이벤트 ID (멱등/추적용)
                orderId,
                reservationKey,
                errorCode,
                errorMessage,
                Instant.now()
        );
    }
}

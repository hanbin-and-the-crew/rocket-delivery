package org.sparta.product.infrastructure.event.kafka.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * 주문 생성 이벤트 (Order 모듈에서 발행), 수신용 이벤트 DTO
 *
 * Product 모듈 관점:
 * - 이 이벤트를 수신하면 재고를 예약한다
 * - 예약 성공 시: StockReservedEvent 발행
 * - 예약 실패 시: StockReservationFailedEvent 발행
 */
public record OrderCreatedEvent (
        UUID eventId,       // 멱등성 보장용
        UUID orderId,
        UUID productId,
        Integer quantity,
        UUID userId,
        Instant occurredAt
) {
}
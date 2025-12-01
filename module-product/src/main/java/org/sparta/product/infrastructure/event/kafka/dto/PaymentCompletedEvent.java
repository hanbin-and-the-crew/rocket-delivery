package org.sparta.product.infrastructure.event.kafka.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * 결제 완료 이벤트 (Payment/Order 모듈에서 발행) 수신용
 *
 * Product 모듈 관점:
 * - 이 이벤트를 수신하면 예약된 재고를 확정(실제 차감)한다
 * - 확정 성공 시: StockConfirmedEvent 발행
 */
public record PaymentCompletedEvent(
        UUID eventId,
        UUID orderId,
        UUID productId,
        Integer quantity,
        Instant occurredAt
) {
}

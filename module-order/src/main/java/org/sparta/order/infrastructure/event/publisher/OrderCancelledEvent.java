package org.sparta.order.infrastructure.event.publisher;

import org.sparta.common.event.DomainEvent;
import org.sparta.order.domain.entity.Order;

import java.time.Instant;
import java.util.UUID;

/**
 * order event 발행
 * 주문 취소 Kafka 이벤트
 * 파일 이름은 product에 있는 것과 동일하게 맞춤
 */
public record OrderCancelledEvent(
        UUID eventId,
        UUID orderId,
        UUID productId,
        Integer quantity,
        Instant occurredAt
) implements DomainEvent {
    /**
     * Create an OrderCancelledEvent representing the given cancelled order.
     *
     * @param order the cancelled Order whose id, product id, and quantity are used to populate the event
     * @return an OrderCancelledEvent with a newly generated `eventId` (for idempotency), the order's `orderId` and `productId`, the order's `quantity`, and the current `occurredAt` timestamp
     */
    public static OrderCancelledEvent of(Order order) {
        return new OrderCancelledEvent(
                UUID.randomUUID(),              // eventId (멱등성 보장용)
                order.getId(),
                order.getProductId(),
                order.getQuantity().getValue(),
                Instant.now()
        );
    }
}
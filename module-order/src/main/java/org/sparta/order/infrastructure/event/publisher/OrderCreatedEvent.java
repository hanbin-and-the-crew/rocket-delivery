package org.sparta.order.infrastructure.event.publisher;

import org.sparta.common.event.DomainEvent;
import org.sparta.order.domain.entity.Order;

import java.time.Instant;
import java.util.UUID;

/**
 * order event 발행
 * 주문 생성 Kafka 이벤트
 */
public record OrderCreatedEvent(
        UUID eventId,       // 멱등성 보장용
        UUID orderId,
        UUID productId,
        Integer quantity,
        UUID userId,
        Instant occurredAt
) implements DomainEvent {
    public static OrderCreatedEvent of(Order order) {
        return new OrderCreatedEvent(
                UUID.randomUUID(),              // eventId (멱등성 보장용)
                order.getId(),
                order.getProductId(),
                order.getQuantity().getValue(),
                order.getCustomerId(),
                Instant.now()
        );
    }
}
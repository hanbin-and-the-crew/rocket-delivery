package org.sparta.order.infrastructure.event.publisher;

import org.sparta.common.event.DomainEvent;
import org.sparta.order.domain.entity.Order;

import java.time.Instant;
import java.util.UUID;

/**
 * order event 발행
 * 주문 삭제 Kafka 이벤트
 */
public record OrderDeletedEvent(
        UUID orderId,
        UUID eventId,
        Instant occurredAt
) implements DomainEvent {
    public static OrderDeletedEvent of(Order order) {
        return new OrderDeletedEvent(
                order.getId(),
                UUID.randomUUID(),              // eventId (멱등성 보장용)
                Instant.now()
        );
    }
}
package org.sparta.common.event.order;

import org.sparta.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * 주문 생성 Spring Event
 */
public record OrderCreatedSpringEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        UUID productId,
        int quantity,
        UUID userId
) implements DomainEvent {

}
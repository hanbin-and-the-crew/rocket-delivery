package org.sparta.order.infrastructure.event.publisher;

import org.sparta.common.event.DomainEvent;
import org.sparta.order.domain.entity.Order;

import java.time.Instant;
import java.util.UUID;

public record OrderApprovedEvent(
        UUID eventId,
        UUID orderId,
        UUID userId,
        Instant occurredAt
) implements DomainEvent {
    /**
     * Create an OrderApprovedEvent for the given order.
     *
     * The created event uses a newly generated UUID for `eventId`, takes `orderId`
     * from `order.getId()`, `userId` from `order.getCustomerId()`, and sets
     * `occurredAt` to the current instant.
     *
     * @param order the order from which to build the event (provides id and customerId)
     * @return an OrderApprovedEvent with a new `eventId`, the order's id and customerId, and the current timestamp
     */
    public static OrderApprovedEvent of(Order order) {
        return new OrderApprovedEvent(
                UUID.randomUUID(),              // eventId (멱등성 보장용)
                order.getId(),
                order.getCustomerId(),
                Instant.now()
        );
    }
}
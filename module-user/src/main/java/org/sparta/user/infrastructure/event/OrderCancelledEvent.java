package org.sparta.user.infrastructure.event;

import java.time.Instant;
import java.util.UUID;

public record OrderCancelledEvent(
        UUID eventId,
        UUID orderId,
        //UUID productId,
        //Integer quantity,
        Instant occurredAt
) {
}
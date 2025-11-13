package org.sparta.order.infrastructure.event.publisher;

import java.util.UUID;

public record OrderQuantityChangedEvent(
        UUID orderId,
        UUID productId,
        int quantity,
        UUID userId) {
}
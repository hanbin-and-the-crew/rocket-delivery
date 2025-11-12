package org.sparta.order.infrastructure.event.dto;

import java.util.UUID;

public record OrderQuantityChangedEvent(
        UUID orderId,
        UUID productId,
        int quantity,
        UUID userId) {
}
package org.sparta.order.infrastructure.event.dto;

import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId,
        UUID productId,
        int quantity,
        UUID userId
) {}
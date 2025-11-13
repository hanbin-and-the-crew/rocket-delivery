package org.sparta.order.infrastructure.event.publisher;

import java.util.UUID;

public record OrderDispatchedEvent(
        UUID orderId,
        UUID productId,
        int quantity) {}
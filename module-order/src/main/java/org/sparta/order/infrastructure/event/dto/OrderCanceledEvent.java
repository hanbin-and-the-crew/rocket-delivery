package org.sparta.order.infrastructure.event.dto;

import java.util.UUID;

public record OrderCanceledEvent(
        UUID orderId,
        UUID productId,
        int quantity) {

}

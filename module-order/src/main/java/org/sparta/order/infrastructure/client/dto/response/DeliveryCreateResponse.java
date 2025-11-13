package org.sparta.order.infrastructure.client.dto.response;

import java.util.UUID;

/**
 * Delivery 서비스로부터 받는 배송 생성 응답
 */
public record DeliveryCreateResponse(
        UUID deliveryId,
        UUID orderId
) {
}
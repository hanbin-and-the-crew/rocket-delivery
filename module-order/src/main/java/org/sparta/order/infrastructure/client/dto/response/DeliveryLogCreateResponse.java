package org.sparta.order.infrastructure.client.dto.response;

import java.util.UUID;

/**
 * Delivery-log 서비스로부터 받는 배송로그 생성 응답
 */
public record DeliveryLogCreateResponse(
        UUID deliveryLogId,
        UUID orderId
) { }

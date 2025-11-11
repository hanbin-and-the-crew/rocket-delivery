package org.sparta.order.infrastructure.client.dto;

import java.util.UUID;

/**
 * Delivery 서비스로 전송하는 배송 생성 요청
 */
public record DeliveryCreateRequest(
        UUID orderId,
        UUID supplierHubId,
        UUID receiptHubId,
        String deliveryAddress
) {
}
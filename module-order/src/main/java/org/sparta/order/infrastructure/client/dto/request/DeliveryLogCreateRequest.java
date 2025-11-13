package org.sparta.order.infrastructure.client.dto.request;

import java.util.UUID;

/**
 * Delivery-log 서비스로 전송하는 배송로그 생성 요청
 */
public record DeliveryLogCreateRequest(
        UUID orderId,
        UUID supplierHubId,
        UUID receiptHubId,
        String deliveryAddress
) { }

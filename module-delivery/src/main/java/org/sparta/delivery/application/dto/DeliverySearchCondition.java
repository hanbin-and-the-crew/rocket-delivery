package org.sparta.delivery.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sparta.delivery.domain.enumeration.DeliveryStatus;

import java.util.UUID;

@Schema(description = "배송 검색 조건")
public record DeliverySearchCondition(
        @Schema(description = "주문 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID orderId,

        @Schema(description = "출발 허브 ID", example = "550e8400-e29b-41d4-a716-446655440001")
        UUID departureHubId,

        @Schema(description = "도착 허브 ID", example = "550e8400-e29b-41d4-a716-446655440002")
        UUID destinationHubId,

        @Schema(description = "배송 상태", example = "HUB_WAITING")
        DeliveryStatus deliveryStatus,

        @Schema(description = "수령인 이름", example = "홍길동")
        String recipientName
) {
    public static DeliverySearchCondition of(
            UUID orderId,
            UUID departureHubId,
            UUID destinationHubId,
            DeliveryStatus deliveryStatus,
            String recipientName
    ) {
        return new DeliverySearchCondition(
                orderId,
                departureHubId,
                destinationHubId,
                deliveryStatus,
                recipientName
        );
    }

    public static DeliverySearchCondition empty() {
        return new DeliverySearchCondition(null, null, null, null, null);
    }
}

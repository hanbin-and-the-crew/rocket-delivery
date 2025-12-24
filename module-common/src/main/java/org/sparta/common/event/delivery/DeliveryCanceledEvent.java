package org.sparta.common.event.delivery;

import org.sparta.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * [ 배송 취소 이벤트 ]
 * DeliveryMan이 수신 받아서 배정된 담당자의 deliveryCount 롤백
 * Order가 수신 받아서 주문 상태 변경 (SHIPPED → CANCELLED)
 * 사용자의 주문 취소의 경우 사용 예정
 */
public record DeliveryCanceledEvent(
        UUID orderId,
        UUID deliveryId,
        UUID hubDeliveryManId,
        UUID companyDeliveryManId,
        UUID eventId,
        Instant occurredAt
) implements DomainEvent {
    public static DeliveryCanceledEvent of(
            UUID orderId,
            UUID deliveryId,
            UUID hubDeliveryManId,
            UUID companyDeliveryManId
    ) {
        return new DeliveryCanceledEvent(
                orderId,
                deliveryId,
                hubDeliveryManId,
                companyDeliveryManId,
                UUID.randomUUID(),
                Instant.now()
        );
    }
}

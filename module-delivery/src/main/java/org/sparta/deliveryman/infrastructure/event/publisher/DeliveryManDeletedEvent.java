package org.sparta.deliveryman.infrastructure.event.publisher;

import org.sparta.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * [ 배송 삭제 이벤트 ]
 * DeliveryMan이 수신 받아서 배정된 담당자의 deliveryCount 롤백
 * Order가 수신 받아서 필요한 처리 수행 (optional)
 */
public record DeliveryManDeletedEvent(
        UUID orderId,
        UUID deliveryId,
        UUID hubDeliveryManId,
        UUID companyDeliveryManId,
        UUID eventId,
        Instant occurredAt
) implements DomainEvent {
    public static DeliveryManDeletedEvent of(
            UUID orderId,
            UUID deliveryId,
            UUID hubDeliveryManId,
            UUID companyDeliveryManId
    ) {
        return new DeliveryManDeletedEvent(
                orderId,
                deliveryId,
                hubDeliveryManId,
                companyDeliveryManId,
                UUID.randomUUID(),
                Instant.now()
        );
    }
}

package org.sparta.deliveryman.infrastructure.event.publisher;

import org.sparta.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * [ 배송 담당자 삭제 이벤트 ]
 * 
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

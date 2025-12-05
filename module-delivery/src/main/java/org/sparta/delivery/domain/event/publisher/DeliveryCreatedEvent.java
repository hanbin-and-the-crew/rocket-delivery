package org.sparta.delivery.domain.event.publisher;

import org.sparta.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

// 배송 생성 완료 이벤트 _ 이것도 아직은 사용 안할듯

public record DeliveryCreatedEvent(
        UUID eventId,
        UUID orderId,
        UUID deliveryId,
        UUID sourceHubId,
        UUID targetHubId,
        Integer totalLogSeq,
        Instant occurredAt
) implements DomainEvent {
    public static DeliveryCreatedEvent of(UUID eventId,UUID orderId, UUID deliveryId, UUID sourceHubId, UUID targetHubId, Integer totalLogSeq, Instant occurredAt) {
        return new DeliveryCreatedEvent(
                UUID.randomUUID(),
                orderId,
                deliveryId,
                sourceHubId,
                targetHubId,
                totalLogSeq,
                Instant.now()

        );
    }
}
package org.sparta.deliveryman.infrastructure.event;

import org.sparta.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

// 배송 생성 완료 이벤트 _ 허브 배송 담당자 배정 Listener에서 사용 됨

public record DeliveryCreatedEvent(
        UUID orderId,
        UUID deliveryId,
        UUID sourceHubId,
        UUID targetHubId,
        Integer totalLogSeq,
        UUID eventId,
        Instant occurredAt
) implements DomainEvent {
    public static DeliveryCreatedEvent of(UUID orderId, UUID deliveryId, UUID sourceHubId, UUID targetHubId, Integer totalLogSeq) {
        return new DeliveryCreatedEvent(
                orderId,
                deliveryId,
                sourceHubId,
                targetHubId,
                totalLogSeq,
                UUID.randomUUID(),
                Instant.now()
        );
    }
}
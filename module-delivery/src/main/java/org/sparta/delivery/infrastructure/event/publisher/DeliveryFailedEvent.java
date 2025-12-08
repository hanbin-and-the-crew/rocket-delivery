package org.sparta.delivery.infrastructure.event.publisher;

import org.sparta.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * [ 배송 실패 이벤트 ]
 * 생성 등 배송의 로직을 수행하면서 실패함을 알리는 이벤트
 */
public record DeliveryFailedEvent(
        UUID orderId,
        String errorReason,
        UUID eventId,
        Instant occurredAt
) implements DomainEvent {
    public static DeliveryFailedEvent of(
            UUID orderId,
            String errorReason
    ) {
        return new DeliveryFailedEvent(
                orderId,
                errorReason,
                UUID.randomUUID(),
                Instant.now()
        );
    }
}

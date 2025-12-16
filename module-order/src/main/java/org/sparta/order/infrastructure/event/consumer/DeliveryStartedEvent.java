package org.sparta.order.infrastructure.event.consumer;

import org.sparta.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * [ Delivery 모듈에서 발행하는 배송 시작 이벤트 ]
 * Order 모듈이 수신하여 주문 상태를 CREATED → APPROVED로 변경
 */
public record DeliveryStartedEvent(
        UUID deliveryId,
        UUID orderId,
        UUID eventId,
        Instant occurredAt
) implements DomainEvent {
    public static DeliveryStartedEvent of(
            UUID deliveryId,
            UUID orderId
    ) {
        return new DeliveryStartedEvent(
                deliveryId,
                orderId,
                UUID.randomUUID(),
                Instant.now()
        );
    }
}

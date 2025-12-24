package org.sparta.order.infrastructure.event.consumer;

import org.sparta.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * [ Delivery 모듈에서 발행하는 최종 배송 완료 이벤트 ]
 * Order 모듈이 수신하여 주문 상태를 APPROVED -> DELIVERED로 변경
 */
public record DeliveryCompletedEvent(
        UUID deliveryId,
        UUID orderId,
        UUID eventId,
        Instant occurredAt
) implements DomainEvent {
    public static DeliveryCompletedEvent of(
            UUID deliveryId,
            UUID orderId
    ) {
        return new DeliveryCompletedEvent(
                deliveryId,
                orderId,
                UUID.randomUUID(),
                Instant.now()
        );
    }
}
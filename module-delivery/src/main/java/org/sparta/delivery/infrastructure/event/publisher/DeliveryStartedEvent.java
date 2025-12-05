package org.sparta.delivery.infrastructure.event.publisher;

import org.sparta.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

// 배송 출발 이벤트 Delivery(HUB_WAITING => HUB_MOVING) / Order(APPROVED => SHIPPED)

public record DeliveryStartedEvent(
        UUID eventId,
        UUID orderId,
        UUID deliveryId,
        UUID sourceHubId,
//        UUID deliveryManId,
        Instant occurredAt
) implements DomainEvent {
    public static DeliveryStartedEvent of(UUID eventId, UUID orderId, UUID deliveryId,UUID sourceHubId, Instant occurredAt) {
        return new DeliveryStartedEvent(
                UUID.randomUUID(),
                orderId,
                deliveryId,
                sourceHubId,
                Instant.now()

        );
    }
}

/**
 * DeliveryStartedEvent / 배송 출발 이벤트
 * 실제로 첫 허브를 출발 할 때 delivery 의 상태 변경 후 이벤트 발행 (HUB_WAITING => HUB_MOVING)
 * Order에서 이 이벤트를 듣고 Order의 상태 변경 (Approved => SHIPPED)
 * Order쪽에서는 SHIPPED 상태가 되면 주문 취소 불가능 상태
 * */

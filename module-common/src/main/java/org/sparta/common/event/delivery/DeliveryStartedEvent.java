package org.sparta.common.event.delivery;

import org.sparta.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

// 배송 출발 이벤트 Delivery(HUB_WAITING => HUB_MOVING) / Order(APPROVED => SHIPPED)

public record DeliveryStartedEvent(
        UUID orderId,
        UUID deliveryId,
        UUID sourceHubId,
//        UUID deliveryManId,
        UUID eventId,
        Instant occurredAt
) implements DomainEvent {
    public static DeliveryStartedEvent of(UUID orderId, UUID deliveryId,UUID sourceHubId) {
        return new DeliveryStartedEvent(
                orderId,
                deliveryId,
                sourceHubId,
                UUID.randomUUID(),
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

package org.sparta.common.event.delivery;

import org.sparta.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

// [ 최종 배송 완료 이벤트 ] _ Order가 수신 받아서 상태 변경 (배송 및 주문 종료)

public record DeliveryCompletedEvent(
        UUID orderId,
        UUID deliveryId,
        UUID receiveCompanyId,
//        Integer actualKm,
//        Integer actualMinutes,
        UUID eventId,
        Instant occurredAt
) implements DomainEvent {
    public static DeliveryCompletedEvent of(UUID orderId, UUID deliveryId, UUID receiveCompanyId) {
        return new DeliveryCompletedEvent(
                orderId,
                deliveryId,
                receiveCompanyId,
                UUID.randomUUID(),
                Instant.now()

        );
    }
}

/**
 * DelieryCompletedEvent : 최종 배송 완료 이벤트
 * Order 상태 변경 (SHIPPED => DELIVERED)
 * Delivery 상태 변경 (COMPANY_MOVING => DELIVERED)
 * */
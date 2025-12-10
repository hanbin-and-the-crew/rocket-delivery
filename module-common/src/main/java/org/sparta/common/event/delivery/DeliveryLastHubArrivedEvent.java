package org.sparta.common.event.delivery;

import org.sparta.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

// [ 목적지 허브 배송 완료 이벤트 ] _ DeliveryMan이 수신 받아서 업체 배송 담당자 배정 시작

public record DeliveryLastHubArrivedEvent(
        UUID orderId,
        UUID deliveryId,
        UUID receiveHubId,
        UUID eventId,
        Instant occurredAt
) implements DomainEvent {
    public static DeliveryLastHubArrivedEvent of(UUID orderId, UUID deliveryId, UUID receiveHubId) {
        return new DeliveryLastHubArrivedEvent(
                orderId,
                deliveryId,
                receiveHubId,
                UUID.randomUUID(),
                Instant.now()

        );
    }
}

/**
 * DeliveryLastHubArrivedEvent : 목적지 허브 배송 완료 이벤트
 * DeliveryMan 업체 배송 담당자 배정 로식 실행 트리거
 * */
//package org.sparta.delivery.domain.event;
//
//import org.sparta.common.event.DomainEvent;
//
//import java.time.Instant;
//import java.util.UUID;
//
//// [ 목적지 허브(최종 허브) 도착 이벤트 ] _ 아직은 사용 안함 Order에서 필요한면 OrderStatus 추가해서 사용
//
//public record DeliveryAtDestHubEvent(
//        UUID eventId,
//        UUID orderId,
//        UUID deliveryId,
//        UUID receiveHubId,
//        Instant occurredAt
//) implements DomainEvent {
//    public static DeliveryAtDestHubEvent of(UUID eventId,UUID orderId, UUID deliveryId, UUID receiveHubId, Instant occurredAt) {
//        return new DeliveryAtDestHubEvent(
//                UUID.randomUUID(),
//                orderId,
//                deliveryId,
//                receiveHubId,
//                Instant.now()
//
//        );
//    }
//}
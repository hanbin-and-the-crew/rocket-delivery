//package org.sparta.delivery.domain.event;
//
//import org.sparta.common.event.DomainEvent;
//
//import java.time.Instant;
//import java.util.UUID;
//
//// [ 업체 배송 시작 이벤트 ] _ 아직 사용 안함 (Order에서 사용하려면 OrderStatus 추가해서 사용해야 됨)
//
//public record CompanyDeliveryStartedEvent(
//        UUID eventId,
//        UUID orderId,
//        UUID deliveryId,
//        UUID fromHubId,
//        UUID deliveryManId,
//        Instant occurredAt
//) implements DomainEvent {
//    public static CompanyDeliveryStartedEvent of(UUID eventId, UUID orderId, UUID deliveryId, UUID fromHubId, UUID deliveryManId, Instant occurredAt) {
//        return new CompanyDeliveryStartedEvent(
//                UUID.randomUUID(),
//                orderId,
//                deliveryId,
//                fromHubId,
//                deliveryManId,
//                Instant.now()
//
//        );
//    }
//}
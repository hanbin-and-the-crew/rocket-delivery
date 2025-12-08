//package org.sparta.delivery.infrastructure.event;
//
//import org.sparta.common.event.DomainEvent;
//import org.sparta.delivery.domain.entity.Delivery;
//import org.sparta.delivery.domain.enumeration.DeliveryStatus;
//
//import java.time.Instant;
//import java.util.UUID;
//
//public record DeliveryCompanyMovingStartEvent(
//        UUID eventId,
//        UUID deliveryId,
//        UUID orderId,
//        UUID companyId,
//        UUID hubId,
//        UUID companyDeliveryManId,
//        DeliveryStatus status,   // COMPANY_MOVING
//        Instant occurredAt
//) implements DomainEvent {
//
//    public static DeliveryCompanyMovingStartEvent from(Delivery delivery) {
//        return new DeliveryCompanyMovingStartEvent(
//                UUID.randomUUID(),
//                delivery.getId(),
//                delivery.getOrderId(),
//                delivery.getReceiveCompanyId(),
//                delivery.getReceiveHubId(),
//                delivery.getCompanyDeliveryManId(),
//                delivery.getStatus(),       // COMPANY_MOVING
//                Instant.now()
//        );
//    }
//}

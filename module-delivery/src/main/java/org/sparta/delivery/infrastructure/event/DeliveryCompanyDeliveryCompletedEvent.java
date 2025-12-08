//package org.sparta.delivery.infrastructure.event;
//
//import org.sparta.common.event.DomainEvent;
//import org.sparta.delivery.domain.entity.Delivery;
//import org.sparta.delivery.domain.enumeration.DeliveryStatus;
//
//import java.time.Instant;
//import java.util.UUID;
//
//public record DeliveryCompanyDeliveryCompletedEvent(
//        UUID eventId,
//        UUID deliveryId,
//        UUID orderId,
//        UUID companyId,
//        UUID hubId,
//        String address,
//        String receiverName,
//        String receiverSlackId,
//        String receiverPhone,
//        DeliveryStatus status,   // DELIVERED
//        Instant occurredAt
//) implements DomainEvent {
//
//    public static DeliveryCompanyDeliveryCompletedEvent from(Delivery delivery) {
//        return new DeliveryCompanyDeliveryCompletedEvent(
//                UUID.randomUUID(),
//                delivery.getId(),
//                delivery.getOrderId(),
//                delivery.getReceiveCompanyId(),
//                delivery.getReceiveHubId(),
//                delivery.getAddress(),
//                delivery.getReceiverName(),
//                delivery.getReceiverSlackId(),
//                delivery.getReceiverPhone(),
//                delivery.getStatus(),   // DELIVERED
//                Instant.now()
//        );
//    }
//}

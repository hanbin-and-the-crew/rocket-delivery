//package org.sparta.delivery.infrastructure.event;
//
//import org.sparta.common.event.DomainEvent;
//import org.sparta.delivery.domain.entity.Delivery;
//import org.sparta.delivery.domain.enumeration.DeliveryStatus;
//
//import java.time.Instant;
//import java.util.UUID;
//
//public record DeliveryCreatedLocalEvent(
//        UUID eventId,
//        UUID deliveryId,
//        UUID orderId,
//        UUID customerId,
//        UUID supplierCompanyId,
//        UUID supplierHubId,
//        UUID receiveCompanyId,
//        UUID receiveHubId,
//        String address,
//        String receiverName,
//        String receiverSlackId,
//        String receiverPhone,
//        DeliveryStatus status,
//        Instant occurredAt
//) implements DomainEvent {
//
//    public static DeliveryCreatedLocalEvent from(Delivery delivery) {
//        return new DeliveryCreatedLocalEvent(
//                UUID.randomUUID(),
//                delivery.getId(),
//                delivery.getOrderId(),
//                delivery.getCustomerId(),
//                delivery.getSupplierCompanyId(),
//                delivery.getSupplierHubId(),
//                delivery.getReceiveCompanyId(),
//                delivery.getReceiveHubId(),
//                delivery.getAddress(),
//                delivery.getReceiverName(),
//                delivery.getReceiverSlackId(),
//                delivery.getReceiverPhone(),
//                delivery.getStatus(),     // CREATED 상태일 것
//                Instant.now()
//        );
//    }
//}

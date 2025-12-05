//package org.sparta.delivery.infrastructure.event;
//
//import org.sparta.common.event.DomainEvent;
//import org.sparta.delivery.domain.entity.Delivery;
//import org.sparta.delivery.domain.enumeration.DeliveryStatus;
//import org.sparta.deliverylog.domain.entity.DeliveryLog;
//import org.sparta.deliverylog.domain.enumeration.DeliveryLogStatus;
//
//import java.time.Instant;
//import java.util.UUID;
//
//public record DeliveryStartEvent(
//        UUID eventId,
//        UUID deliveryId,
//        UUID orderId,
//        int sequence,
//        UUID sourceHubId,
//        UUID targetHubId,
//        UUID hubDeliveryManId,
//        DeliveryStatus deliveryStatus,
//        DeliveryLogStatus logStatus,
//        Instant occurredAt
//) implements DomainEvent {
//
//    public static DeliveryStartEvent from(Delivery delivery, DeliveryLog log) {
//        return new DeliveryStartEvent(
//                UUID.randomUUID(),
//                delivery.getId(),
//                delivery.getOrderId(),
//                log.getSequence(),
//                log.getSourceHubId(),
//                log.getTargetHubId(),
//                log.getDeliveryManId(),           // 이 leg 담당 허브 배송 담당자
//                delivery.getStatus(),             // HUB_MOVING
//                log.getStatus(),                  // HUB_MOVING
//                Instant.now()
//        );
//    }
//}

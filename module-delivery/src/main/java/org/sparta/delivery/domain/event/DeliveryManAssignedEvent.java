//package org.sparta.delivery.domain.event;
//
//import org.sparta.common.event.DomainEvent;
//
//import java.time.Instant;
//import java.util.UUID;
//
//// 배송 담당자 배정 완료 이벤트 _ 이 벤트는 일단은 사용할 필요은 없을 것 같긴 한데 일단 event dto만 만들어 놨음
//
//public record DeliveryManAssignedEvent(
//        UUID eventId,
//        UUID orderId,
//        UUID deliveryId,
////        UUID hubDeliveryManId,
////        UUID companyDeliveryManId,
//        Instant occurredAt
//) implements DomainEvent {
//    public static DeliveryManAssignedEvent of(UUID eventId,UUID orderId, UUID deliveryId, Instant occurredAt) {
//        return new DeliveryManAssignedEvent(
//                UUID.randomUUID(),
//                orderId,
//                deliveryId,
//                Instant.now()
//
//        );
//    }
//}
//
///**
// * DelvieryManAssignedEvent 담당자 배정 완료 이벤트
// * 담당자가 배정되면 CREAETED => HUB_WAITING으로 상태가 변경됨
// * 이때부터 허브 출발이 가능해짐
// * */
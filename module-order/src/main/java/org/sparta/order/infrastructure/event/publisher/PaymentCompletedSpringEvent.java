//package org.sparta.order.infrastructure.event.publisher;
//
//import org.sparta.common.event.DomainEvent;
//import org.sparta.order.domain.entity.Payment;
//
//import java.time.Instant;
//import java.util.UUID;
//
///**
// * 결제 생성 Spring Event
// */
//public record PaymentCompletedSpringEvent(
//        UUID eventId,
//        Instant occurredAt,
//        UUID paymentId,
//        UUID productId,
//        int quantity,
//        UUID userId
//) implements DomainEvent {
//    public static PaymentCompletedSpringEvent of(Payment payment, UUID userId) {
//        return new PaymentCompletedSpringEvent(
//                java.util.UUID.randomUUID(),              // eventId (멱등성 보장용)
//                Instant.now(),
//                payment.getId(),
//                payment.getProductId(),
//                payment.getQuantity(),
//                userId // Order Entity에는 userId가 없으므로 따로 받아준다.
//        );
//    }
//}
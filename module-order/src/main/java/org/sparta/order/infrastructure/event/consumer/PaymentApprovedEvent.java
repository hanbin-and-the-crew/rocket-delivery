package org.sparta.order.infrastructure.event.consumer;

import org.sparta.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * [ Payment 모듈에서 발행하는 결제 승인 이벤트 ]
 * Order 모듈이 수신하여 주문 상태를 CREATED → APPROVED로 변경k 및 event 발행
 */
public record PaymentApprovedEvent(
        UUID paymentId,
        UUID orderId,
        UUID customerId,
        Long totalPrice,
//        String paymentMethod,
        UUID eventId,
        Instant occurredAt
) implements DomainEvent {
    public static PaymentApprovedEvent of(
            UUID paymentId,
            UUID orderId,
            UUID customerId,
            Long totalPrice
//            String paymentMethod
    ) {
        return new PaymentApprovedEvent(
                paymentId,
                orderId,
                customerId,
                totalPrice,
//                paymentMethod,
                UUID.randomUUID(),
                Instant.now()
        );
    }
}

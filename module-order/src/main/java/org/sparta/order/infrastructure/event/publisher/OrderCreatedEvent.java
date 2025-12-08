package org.sparta.order.infrastructure.event.publisher;

import org.sparta.common.event.DomainEvent;
import org.sparta.order.domain.entity.Order;

import java.time.Instant;
import java.util.UUID;

/**
 * 주문 생성 완료 + 결제/포인트/쿠폰 정보 포함 이벤트
 */
public record OrderCreatedEvent(
        UUID orderId,
        Long orderAmount,     // 주문 총 금액 (상품가 * 수량)
        Long requestPoint,       // 사용 포인트
        Long discountAmount,// 사용 쿠폰 할인 금액
        Long amountPayable,   // 실 PG 결제 금액
        String pointReservationId,
        String couponReservationId,
        String pgToken,
        UUID eventId,
        Instant occurredAt
) implements DomainEvent {

    public static OrderCreatedEvent of(
            UUID orderId,
            Long orderAmount,
            Long requestPoint,
            Long discountAmount,
            Long amountPayable,
            String pointReservationId,
            String couponReservationId,
            String pgToken
    ) {
        return new OrderCreatedEvent (
                orderId,
                orderAmount,
                requestPoint,
                discountAmount,
                amountPayable,
                pointReservationId,
                couponReservationId,
                pgToken,
                UUID.randomUUID(),
                Instant.now()
        );
    }
}
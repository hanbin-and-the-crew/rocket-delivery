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
        Long amountTotal,   // 주문 금액 (단가 * 수량)
        Long amountCoupon,  // 쿠폰 사용금액
        Long amountPoint,   // 포인트 사용금액
        Long amountPayable, // 실제 결제 금액 (주문 금액 - 쿠폰 - 포인트)
        String methodType,  // 결제 수단
        String pgProvider,  // PG사
        String currency,    // 화폐
        UUID couponId,      // 사용한 쿠폰 id
        UUID pointUsageId,  // 사용한 포인트 예약 id
        String paymentKey,  // pg 결제 후 받은 key
        UUID eventId,
        Instant occurredAt
) implements DomainEvent {


    public static OrderCreatedEvent of(
            UUID orderId,
            Long amountTotal,
            Long amountCoupon,
            Long amountPoint,
            Long amountPayable,
            String methodType,
            String pgProvider,
            String currency,
            UUID couponId,
            UUID pointUsageId,
            String paymentKey
    ) {
        return new OrderCreatedEvent (
                orderId,
                amountTotal,
                amountCoupon,
                amountPoint,
                amountPayable,
                methodType,
                pgProvider,
                currency,
                couponId,
                pointUsageId,
                paymentKey,
                UUID.randomUUID(),
                Instant.now()
        );
    }
}
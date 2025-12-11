package org.sparta.common.event.payment;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 결제 성공(PAYMENT_COMPLETED) 이벤트 페이로드
 * - 다른 서비스(주문, 정산, 포인트, 쿠폰 등)가 참조할 수 있는 정보들만 담는다.
 * - domain(PaymentType, PgProvider 등)을 참조하지 않도록 모든 값은 primitive/String/UUID 로 유지한다.
 */
public record PaymentCompletedEvent(
        UUID paymentId,
        UUID orderId,
        Long amountTotal,
        Long amountCoupon,
        Long amountPoint,
        Long amountPayable,
        Long amountPaid,
        String currency,
        String methodType,
        String pgProvider,
        String paymentKey,
        LocalDateTime approvedAt,
        UUID couponId,
        UUID pointUsageId
) {
}

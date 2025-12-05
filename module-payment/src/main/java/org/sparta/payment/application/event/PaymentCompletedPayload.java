package org.sparta.payment.application.event;
import java.time.LocalDateTime;
import java.util.UUID;

import org.sparta.payment.domain.entity.Payment;
import org.sparta.payment.domain.enumeration.PaymentType;
import org.sparta.payment.domain.enumeration.PgProvider;


/**
 * 결제 성공(PAYMENT_COMPLETED) 이벤트 페이로드
 * - 다른 서비스(주문, 정산, 포인트 등)가 참조할 수 있는 정보들만 담는다.
 */
public record PaymentCompletedPayload(
        UUID paymentId,
        UUID orderId,
        Long amountTotal,
        Long amountCoupon,
        Long amountPoint,
        Long amountPayable,
        Long amountPaid,
        String currency,
        PaymentType methodType,
        PgProvider pgProvider,
        String paymentKey,
        LocalDateTime approvedAt,
        UUID couponId,
        UUID pointUsageId
) {
    public static PaymentCompletedPayload from(Payment payment) {
        return new PaymentCompletedPayload(
                payment.getPaymentId(),
                payment.getOrderId(),
                payment.getAmountTotal(),
                payment.getAmountCoupon(),
                payment.getAmountPoint(),
                payment.getAmountPayable(),
                payment.getAmountPaid(),
                payment.getCurrency(),
                payment.getMethodType(),
                payment.getPgProvider(),
                payment.getPaymentKey(),
                payment.getApprovedAt(),
                payment.getCouponId(),
                payment.getPointUsageId()
        );
    }
}

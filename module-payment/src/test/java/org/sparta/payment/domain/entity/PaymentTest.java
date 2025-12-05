package org.sparta.payment.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.common.error.BusinessException;
import org.sparta.payment.domain.enumeration.PaymentStatus;
import org.sparta.payment.domain.enumeration.PaymentType;
import org.sparta.payment.domain.enumeration.PgProvider;
import org.sparta.payment.domain.error.PaymentErrorType;

class PaymentTest {

    @Test
    @DisplayName("createRequested - 정상 금액이면 REQUESTED 상태로 생성된다")
    void createRequested_success() {
        // given
        UUID orderId = UUID.randomUUID();
        long amountTotal = 10000L;
        long amountCoupon = 2000L;
        long amountPoint = 3000L;
        long amountPayable = 5000L; // 2,000 + 3,000 + 5,000 = 10,000

        // when
        Payment payment = Payment.createRequested(
                orderId,
                amountTotal,
                amountCoupon,
                amountPoint,
                amountPayable,
                PaymentType.CARD,
                PgProvider.TOSS,
                "KRW",
                null,
                null
        );

        // then

        assertThat(payment.getOrderId()).isEqualTo(orderId);
        assertThat(payment.getAmountTotal()).isEqualTo(amountTotal);
        assertThat(payment.getAmountCoupon()).isEqualTo(amountCoupon);
        assertThat(payment.getAmountPoint()).isEqualTo(amountPoint);
        assertThat(payment.getAmountPayable()).isEqualTo(amountPayable);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REQUESTED);
        assertThat(payment.getRequestedAt()).isNotNull();
    }

    @Test
    @DisplayName("createRequested - total이 0 이하이면 INVALID_AMOUNT 에러")
    void createRequested_invalidTotal() {
        // given
        UUID orderId = UUID.randomUUID();

        // when & then
        assertThatThrownBy(() ->
                Payment.createRequested(
                        orderId,
                        0L,
                        0L,
                        0L,
                        0L,
                        PaymentType.CARD,
                        PgProvider.TOSS,
                        "KRW",
                        null,
                        null
                )
        )
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorType", PaymentErrorType.INVALID_AMOUNT);
    }

    @Test
    @DisplayName("createRequested - 쿠폰+포인트+결제금액 합이 total과 다르면 PAYMENT_AMOUNT_MISMATCH")
    void createRequested_amountMismatch() {
        // given
        UUID orderId = UUID.randomUUID();

        // when & then
        assertThatThrownBy(() ->
                Payment.createRequested(
                        orderId,
                        10000L,
                        1000L,
                        1000L,
                        5000L,   // 1,000 + 1,000 + 5,000 = 7,000 != 10,000
                        PaymentType.CARD,
                        PgProvider.TOSS,
                        "KRW",
                        null,
                        null
                )
        )
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorType", PaymentErrorType.PAYMENT_AMOUNT_MISMATCH);
    }

    @Test
    @DisplayName("complete - 결제가 성공하면 COMPLETED 상태가 되고 approvedAt/amountPaid 세팅된다")
    void complete_success() {
        // given
        Payment payment = Payment.createRequested(
                UUID.randomUUID(),
                10000L,
                2000L,
                3000L,
                5000L,
                PaymentType.CARD,
                PgProvider.TOSS,
                "KRW",
                null,
                null
        );
        String paymentKey = "pay_123456789";

        // when
        payment.complete(paymentKey, 5000L);

        // then
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getPaymentKey()).isEqualTo(paymentKey);
        assertThat(payment.getAmountPaid()).isEqualTo(5000L);
        assertThat(payment.getApprovedAt()).isNotNull();
    }

    @Test
    @DisplayName("cancelAll - 전체 취소 시 CANCELED 상태이며 canceledAt이 세팅된다")
    void cancelAll_setsCanceled() {
        // given
        Payment payment = Payment.createRequested(
                UUID.randomUUID(),
                10000L,
                0L,
                0L,
                10000L,
                PaymentType.CARD,
                PgProvider.TOSS,
                "KRW",
                null,
                null
        );
        payment.complete("pay_key", 10000L);

        // when
        payment.cancelAll();

        // then
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(payment.getCanceledAt()).isNotNull();
    }

    @Test
    @DisplayName("applyRefund - 환불을 적용하면 비즈니스 규칙에 맞게 처리된다(예: 부분 환불에도 amountPaid 유지 등)")
    void applyRefund() {
        // given
        Payment payment = Payment.createRequested(
                UUID.randomUUID(),
                10000L,
                0L,
                0L,
                10000L,
                PaymentType.CARD,
                PgProvider.TOSS,
                "KRW",
                null,
                null
        );
        payment.complete("pay_key", 10000L);

        long refundAmount = 3000L;

        // when
        payment.applyRefund(refundAmount);

        // then
        // 여기는 네가 정의한 규칙에 맞춰 assert 수정:
        // 예: 부분 환불이어도 amountPaid는 원래 승인 금액 유지, 정산에서만 조정 등
        assertThat(payment.getStatus()).isIn(PaymentStatus.COMPLETED, PaymentStatus.CANCELED);
        // 필요하면 refund 누적 금액/상태 등 추가 검증
    }
}

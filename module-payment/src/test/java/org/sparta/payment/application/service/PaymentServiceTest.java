package org.sparta.payment.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sparta.common.error.BusinessException;
import org.sparta.payment.application.command.payment.PaymentCancelCommand;
import org.sparta.payment.application.command.payment.PaymentCreateCommand;
import org.sparta.payment.application.dto.PaymentDetailResult;
import org.sparta.payment.domain.entity.Payment;
import org.sparta.payment.domain.entity.PaymentOutbox;
import org.sparta.payment.domain.entity.Refund;
import org.sparta.common.domain.OutboxStatus;
import org.sparta.payment.domain.enumeration.PaymentStatus;
import org.sparta.common.domain.PaymentType;
import org.sparta.common.domain.PgProvider;
import org.sparta.payment.domain.error.PaymentErrorType;
import org.sparta.payment.domain.repository.PaymentOutboxRepository;
import org.sparta.payment.domain.repository.PaymentRepository;
import org.sparta.payment.domain.repository.RefundRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private PaymentOutboxRepository outboxRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    @DisplayName("storeCompletedPayment - 정상 호출 시 Payment 저장 및 Outbox 생성")
    void storeCompletedPayment_success() throws Exception {
        // given
        UUID orderId = UUID.randomUUID();
        String paymentKey = "pay_key";

        PaymentCreateCommand command = new PaymentCreateCommand(
                orderId,
                10_000L,  // amountTotal
                2_000L,   // amountCoupon
                3_000L,   // amountPoint
                5_000L,   // amountPayable
                PaymentType.CARD,
                PgProvider.TOSS,
                "KRW",
                null,
                null
        );

        // 멱등성 체크: 동일 paymentKey 로 기존 결제가 없다고 가정
        when(paymentRepository.findByPaymentKey(paymentKey))
                .thenReturn(Optional.empty());

        // 저장 후 반환될 Payment 모의 객체
        Payment savedMock = mock(Payment.class);
        UUID paymentId = UUID.randomUUID();
        when(savedMock.getPaymentId()).thenReturn(paymentId);
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedMock);

        // ObjectMapper 직렬화는 단순 문자열로 stub
        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"dummy\":\"json\"}");

        ArgumentCaptor<PaymentOutbox> outboxCaptor = ArgumentCaptor.forClass(PaymentOutbox.class);

        // when
        PaymentDetailResult result = paymentService.storeCompletedPayment(command, paymentKey);

        // then
        // Payment 저장 1회
        verify(paymentRepository, times(1)).save(any(Payment.class));

        // Outbox 저장 1회, 캡쳐
        verify(outboxRepository, times(1)).save(outboxCaptor.capture());
        PaymentOutbox outbox = outboxCaptor.getValue();

        // Outbox 필드 검증
        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.READY);
        assertThat(outbox.getAggregateType()).isEqualTo("PAYMENT");
        assertThat(outbox.getAggregateId()).isEqualTo(paymentId);
        assertThat(outbox.getEventType()).isEqualTo("payment.orderCreate.paymentCompleted");
        assertThat(outbox.getPayload()).isEqualTo("{\"dummy\":\"json\"}");

        // 결과 객체가 null 이 아니고, paymentId 가 세팅되어 있는지 정도까지 확인
        assertThat(result).isNotNull();
        assertThat(result.paymentId()).isEqualTo(paymentId);
    }

    @Test
    @DisplayName("storeCompletedPayment - 금액 합이 맞지 않으면 PAYMENT_AMOUNT_MISMATCH 예외 및 PAYMENT_FAILED Outbox 생성")
    void storeCompletedPayment_invalidAmounts() throws Exception {
        // given
        UUID orderId = UUID.randomUUID();
        String paymentKey = "pay_key_invalid";

        // amountTotal != coupon + point + payable
        PaymentCreateCommand command = new PaymentCreateCommand(
                orderId,
                10_000L,  // amountTotal
                1_000L,   // amountCoupon
                1_000L,   // amountPoint
                5_000L,   // amountPayable  -> 합계 7,000 != 10,000
                PaymentType.CARD,
                PgProvider.TOSS,
                "KRW",
                null,
                null
        );

        when(paymentRepository.findByPaymentKey(paymentKey))
                .thenReturn(Optional.empty());

        // 실패 이벤트 직렬화는 간단한 JSON 문자열로 stub
        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"eventType\":\"payment.orderCreateFail.paymentFail\"}");

        ArgumentCaptor<PaymentOutbox> outboxCaptor = ArgumentCaptor.forClass(PaymentOutbox.class);

        // when & then
        assertThatThrownBy(() -> paymentService.storeCompletedPayment(command, paymentKey))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorType()).isEqualTo(PaymentErrorType.PAYMENT_AMOUNT_MISMATCH);
                });

        // 결제 엔티티는 저장되지 않는다.
        verify(paymentRepository, never()).save(any());

        // 대신 실패 이벤트(PAYMENT_FAILED)가 Outbox 로 한 번 저장된다.
        verify(outboxRepository, times(1)).save(outboxCaptor.capture());
        PaymentOutbox outbox = outboxCaptor.getValue();
        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.READY);
        assertThat(outbox.getEventType()).isEqualTo("payment.orderCreateFail.paymentFail");
        assertThat(outbox.getAggregateType()).isEqualTo("PAYMENT");
        assertThat(outbox.getAggregateId()).isEqualTo(orderId);
        assertThat(outbox.getPayload()).isEqualTo("{\"eventType\":\"payment.orderCreateFail.paymentFail\"}");
    }


    @Test
    @DisplayName("cancelPayment - COMPLETED 상태 결제는 전체 취소 및 환불이 생성된다")
    void cancelPayment_success() {
        // given
        UUID paymentId = UUID.randomUUID();

        // Payment 엔티티는 mock으로 사용 (상태/금액만 쓰므로)
        Payment paymentMock = mock(Payment.class);
        when(paymentMock.getStatus()).thenReturn(PaymentStatus.COMPLETED);
        when(paymentMock.getAmountPaid()).thenReturn(5_000L);   // 실제 결제 금액
        when(paymentMock.getAmountPayable()).thenReturn(5_000L);
        when(paymentMock.getPaymentId()).thenReturn(paymentId);

        // 저장 후 반환도 동일 mock으로
        when(paymentRepository.findById(paymentId))
                .thenReturn(Optional.of(paymentMock));
        when(paymentRepository.save(any(Payment.class)))
                .thenReturn(paymentMock);

        PaymentCancelCommand command = new PaymentCancelCommand(
                paymentId,
                "사용자 요청 취소"
        );

        ArgumentCaptor<Refund> refundCaptor = ArgumentCaptor.forClass(Refund.class);

        // when
        PaymentDetailResult result = paymentService.cancelPayment(command);

        // then
        // 환불 한 번 생성
        verify(refundRepository, times(1)).save(refundCaptor.capture());
        Refund refund = refundCaptor.getValue();
        assertThat(refund.getPaymentId()).isEqualTo(paymentId);
        assertThat(refund.getAmount()).isEqualTo(5_000L);
        assertThat(refund.getReason()).isEqualTo("사용자 요청 취소");

        // 결제 저장도 한 번 호출
        verify(paymentRepository, times(1)).save(any(Payment.class));

        // cancelAll 이 호출되었는지까지 보고 싶으면
        verify(paymentMock, times(1)).cancelAll();

        // 결과 객체가 null이 아닌지만 간단히 확인
        assertThat(result).isNotNull();
    }


    @Test
    @DisplayName("cancelPayment - 이미 CANCELED 또는 REFUNDED 인 경우 PAYMENT_ALREADY_CANCELED 예외")
    void cancelPayment_alreadyCanceledOrRefunded() {
        // given
        UUID paymentId = UUID.randomUUID();

        // 이미 취소된 결제라고 가정
        Payment paymentMock = mock(Payment.class);
        when(paymentMock.getStatus()).thenReturn(PaymentStatus.CANCELED);

        // cancelPayment 실패 시 실패 Outbox 를 만들 때 필요한 필드들 세팅 (NPE 방지)
        UUID orderId = UUID.randomUUID();
        when(paymentMock.getOrderId()).thenReturn(orderId);
        when(paymentMock.getAmountPayable()).thenReturn(5_000L);
        when(paymentMock.getCurrency()).thenReturn("KRW");

        when(paymentRepository.findById(paymentId))
                .thenReturn(Optional.of(paymentMock));

        PaymentCancelCommand command = new PaymentCancelCommand(
                paymentId,
                "중복 취소 요청"
        );

        // when & then
        assertThatThrownBy(() -> paymentService.cancelPayment(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorType())
                            .isEqualTo(PaymentErrorType.PAYMENT_ALREADY_CANCELED);
                });

        // 환불/저장 로직은 전혀 타면 안 된다
        verify(refundRepository, never()).save(any());
        verify(paymentRepository, never()).save(any());
    }
}

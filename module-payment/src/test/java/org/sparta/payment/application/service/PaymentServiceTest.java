package org.sparta.payment.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sparta.common.error.BusinessException;

import org.sparta.payment.application.command.payment.PaymentCreateCommand;
import org.sparta.payment.domain.entity.Payment;
import org.sparta.payment.domain.entity.PaymentOutbox;
import org.sparta.payment.domain.enumeration.OutboxStatus;
import org.sparta.payment.domain.enumeration.PaymentType;
import org.sparta.payment.domain.enumeration.PgProvider;
import org.sparta.payment.domain.error.PaymentErrorType;
import org.sparta.payment.domain.repository.PaymentOutboxRepository;
import org.sparta.payment.domain.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentOutboxRepository outboxRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PaymentService paymentService;
/*
    @Test
    @DisplayName("storeCompletedPayment - 금액 합이 맞지 않으면 PAYMENT_AMOUNT_MISMATCH 예외")
    void storeCompletedPayment_invalidAmounts() {
        // given
        PaymentCreateCommand command = new PaymentCreateCommand(
                UUID.randomUUID(),
                10000L,
                1000L,
                1000L,
                5000L, // 1,000 + 1,000 + 5,000 != 10,000
                PaymentType.CARD,
                PgProvider.TOSS,
                "KRW",
                null,
                null
        );

        // when & then
        assertThatThrownBy(() ->
                paymentService.storeCompletedPayment(command, "pay_key")
        )
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorType", PaymentErrorType.PAYMENT_AMOUNT_MISMATCH);

        verifyNoInteractions(paymentRepository, outboxRepository);
    }*/

    @Test
    @DisplayName("storeCompletedPayment - 정상 호출 시 Payment 저장 및 Outbox 생성")
    void storeCompletedPayment_success() throws Exception {
        // given
        PaymentCreateCommand command = new PaymentCreateCommand(
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

        Payment savedMock = mock(Payment.class);
        when(savedMock.getPaymentId()).thenReturn(UUID.randomUUID());
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedMock);

        // ObjectMapper 가 JSON 직렬화하는 부분은 단순 문자열로 stub
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"dummy\":\"json\"}");

        ArgumentCaptor<PaymentOutbox> outboxCaptor = ArgumentCaptor.forClass(PaymentOutbox.class);

        // when
        paymentService.storeCompletedPayment(command, "pay_key");

        // then
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(outboxRepository, times(1)).save(outboxCaptor.capture());

        PaymentOutbox outbox = outboxCaptor.getValue();
        // READY 상태로 저장되는지, eventType/aggregateId 등은 필요시 추가 검증
        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.READY);
        // 필요하면 eventType, aggregateType, payload 등 assert 추가
    }
}

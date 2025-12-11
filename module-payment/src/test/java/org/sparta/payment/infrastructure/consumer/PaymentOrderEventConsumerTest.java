package org.sparta.payment.infrastructure.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sparta.common.domain.PaymentType;
import org.sparta.common.domain.PgProvider;
import org.sparta.common.error.BusinessException;
import org.sparta.common.event.order.OrderCancelledEvent;
import org.sparta.common.event.order.OrderCreatedEvent;
import org.sparta.payment.application.command.payment.PaymentCancelCommand;
import org.sparta.payment.application.command.payment.PaymentCreateCommand;
import org.sparta.payment.application.command.payment.PaymentGetByOrderIdCommand;
import org.sparta.payment.application.dto.PaymentDetailResult;
import org.sparta.payment.application.service.PaymentService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PaymentOrderEventConsumer 에 대한 단위 테스트.
 * - Kafka 인프라는 사용하지 않고, 리스너 메서드를 직접 호출하는 방식으로 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class PaymentOrderEventConsumerTest {

    @Mock
    private PaymentService paymentService;

    // ObjectMapper 는 현재 Consumer 에서 사용하지 않지만, 생성자 의존성에 맞추기 위해 준비
    private ObjectMapper objectMapper;

    @InjectMocks
    private org.sparta.payment.infrastructure.consumer.PaymentOrderEventConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        // @InjectMocks 로 이미 인스턴스가 만들어지지만,
        // ObjectMapper 를 명시적으로 주입하고 싶다면 생성자를 직접 호출해도 된다.
        consumer = new org.sparta.payment.infrastructure.consumer.PaymentOrderEventConsumer(
                objectMapper,
                paymentService
        );
    }

    @Test
    @DisplayName("orderCreateConsum - 정상 처리 시 PaymentService.storeCompletedPayment 가 호출된다")
    void orderCreateConsum_success() {
        // given
        UUID orderId = UUID.randomUUID();
        String paymentKey = "pay_1234";
        Long amountTotal = 10_000L;
        Long amountCoupon = 1_000L;
        Long amountPoint = 2_000L;
        Long amountPayable = 7_000L;
        String methodTypeStr = "card";   // 소문자로 들어와도 toUpperCase 로 매핑 가능
        String pgProviderStr = "toss";
        String currency = "KRW";
        UUID couponId = UUID.randomUUID();
        UUID pointUsageId = UUID.randomUUID();

        OrderCreatedEvent event = mock(OrderCreatedEvent.class);
        when(event.orderId()).thenReturn(orderId);
        when(event.paymentKey()).thenReturn(paymentKey);
        when(event.amountTotal()).thenReturn(amountTotal);
        when(event.amountCoupon()).thenReturn(amountCoupon);
        when(event.amountPoint()).thenReturn(amountPoint);
        when(event.amountPayable()).thenReturn(amountPayable);
        when(event.methodType()).thenReturn(methodTypeStr);
        when(event.pgProvider()).thenReturn(pgProviderStr);
        when(event.currency()).thenReturn(currency);
        when(event.couponId()).thenReturn(couponId);
        when(event.pointUsageId()).thenReturn(pointUsageId);

        // when
        consumer.orderCreateConsum(event);

        // then
        verify(paymentService, times(1))
                .storeCompletedPayment(
                        argThat(matchesCreateCommand(
                                orderId,
                                amountTotal,
                                amountCoupon,
                                amountPoint,
                                amountPayable,
                                PaymentType.CARD,
                                PgProvider.TOSS,
                                currency,
                                couponId,
                                pointUsageId
                        )),
                        eq(paymentKey)
                );
    }

    @Test
    @DisplayName("orderCreateConsum - BusinessException 발생 시 예외를 전파하지 않는다")
    void orderCreateConsum_businessException() {
        // given
        OrderCreatedEvent event = mock(OrderCreatedEvent.class);
        when(event.orderId()).thenReturn(UUID.randomUUID());
        when(event.paymentKey()).thenReturn("pay_1234");
        when(event.amountTotal()).thenReturn(10_000L);
        when(event.amountCoupon()).thenReturn(1_000L);
        when(event.amountPoint()).thenReturn(2_000L);
        when(event.amountPayable()).thenReturn(7_000L);
        when(event.methodType()).thenReturn("card");
        when(event.pgProvider()).thenReturn("toss");
        when(event.currency()).thenReturn("KRW");
        when(event.couponId()).thenReturn(UUID.randomUUID());
        when(event.pointUsageId()).thenReturn(UUID.randomUUID());

        BusinessException ex = mock(BusinessException.class);
        doThrow(ex).when(paymentService).storeCompletedPayment(any(PaymentCreateCommand.class), anyString());

        // when
        consumer.orderCreateConsum(event);

        // then
        verify(paymentService, times(1))
                .storeCompletedPayment(any(PaymentCreateCommand.class), anyString());
        // 예외가 밖으로 던져지지 않아야 한다 (테스트 메서드가 실패하지 않음으로 검증)
    }

    @Test
    @DisplayName("orderCreateConsum - 시스템 예외 발생 시 예외를 그대로 전파한다")
    void orderCreateConsum_systemException() {
        // given
        OrderCreatedEvent event = mock(OrderCreatedEvent.class);
        when(event.orderId()).thenReturn(UUID.randomUUID());
        when(event.paymentKey()).thenReturn("pay_1234");
        when(event.amountTotal()).thenReturn(10_000L);
        when(event.amountCoupon()).thenReturn(1_000L);
        when(event.amountPoint()).thenReturn(2_000L);
        when(event.amountPayable()).thenReturn(7_000L);
        when(event.methodType()).thenReturn("card");
        when(event.pgProvider()).thenReturn("toss");
        when(event.currency()).thenReturn("KRW");
        when(event.couponId()).thenReturn(UUID.randomUUID());
        when(event.pointUsageId()).thenReturn(UUID.randomUUID());

        RuntimeException ex = new RuntimeException("DB error");
        doThrow(ex).when(paymentService).storeCompletedPayment(any(PaymentCreateCommand.class), anyString());

        // then
        assertThatThrownBy(() -> consumer.orderCreateConsum(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB error");
    }

    @Test
    @DisplayName("orderCancelConsum - 정상 처리 시 getPaymentByOrderId 와 cancelPayment 가 호출된다")
    void orderCancelConsum_success() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        OrderCancelledEvent event = mock(OrderCancelledEvent.class);
        when(event.orderId()).thenReturn(orderId);

        PaymentDetailResult detailResult = mock(PaymentDetailResult.class);
        when(detailResult.paymentId()).thenReturn(paymentId);

        when(paymentService.getPaymentByOrderId(any(PaymentGetByOrderIdCommand.class)))
                .thenReturn(detailResult);

        // when
        consumer.orderCancelConsum(event);

        // then
        verify(paymentService, times(1))
                .getPaymentByOrderId(
                        argThat(cmd -> orderId.equals(cmd.orderId()))
                );

        verify(paymentService, times(1))
                .cancelPayment(
                        argThat(cmd ->
                                paymentId.equals(cmd.paymentId())
                                        && "ORDER_CANCELLED".equals(cmd.reason())
                        )
                );
    }

    @Test
    @DisplayName("orderCancelConsum - BusinessException 발생 시 예외를 전파하지 않는다")
    void orderCancelConsum_businessException() {
        // given
        UUID orderId = UUID.randomUUID();

        OrderCancelledEvent event = mock(OrderCancelledEvent.class);
        when(event.orderId()).thenReturn(orderId);

        PaymentDetailResult detailResult = mock(PaymentDetailResult.class);
        when(detailResult.paymentId()).thenReturn(UUID.randomUUID());

        when(paymentService.getPaymentByOrderId(any(PaymentGetByOrderIdCommand.class)))
                .thenReturn(detailResult);

        BusinessException ex = mock(BusinessException.class);
        doThrow(ex).when(paymentService).cancelPayment(any(PaymentCancelCommand.class));

        // when
        consumer.orderCancelConsum(event);

        // then
        verify(paymentService, times(1))
                .cancelPayment(any(PaymentCancelCommand.class));
        // 예외가 밖으로 던져지지 않아야 한다
    }

    @Test
    @DisplayName("orderCancelConsum - 시스템 예외 발생 시 예외를 그대로 전파한다")
    void orderCancelConsum_systemException() {
        // given
        UUID orderId = UUID.randomUUID();

        OrderCancelledEvent event = mock(OrderCancelledEvent.class);
        when(event.orderId()).thenReturn(orderId);

        PaymentDetailResult detailResult = mock(PaymentDetailResult.class);
        when(detailResult.paymentId()).thenReturn(UUID.randomUUID());

        when(paymentService.getPaymentByOrderId(any(PaymentGetByOrderIdCommand.class)))
                .thenReturn(detailResult);

        RuntimeException ex = new RuntimeException("DB error");
        doThrow(ex).when(paymentService).cancelPayment(any(PaymentCancelCommand.class));

        // then
        assertThatThrownBy(() -> consumer.orderCancelConsum(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB error");
    }

    /**
     * PaymentCreateCommand 가 기대한 값들로 만들어졌는지 검증하기 위한 ArgumentMatcher
     */
    private ArgumentMatcher<PaymentCreateCommand> matchesCreateCommand(
            UUID orderId,
            Long amountTotal,
            Long amountCoupon,
            Long amountPoint,
            Long amountPayable,
            PaymentType methodType,
            PgProvider pgProvider,
            String currency,
            UUID couponId,
            UUID pointUsageId
    ) {
        return cmd ->
                cmd != null
                        && orderId.equals(cmd.orderId())
                        && amountTotal.equals(cmd.amountTotal())
                        && amountCoupon.equals(cmd.amountCoupon())
                        && amountPoint.equals(cmd.amountPoint())
                        && amountPayable.equals(cmd.amountPayable())
                        && methodType == cmd.methodType()
                        && pgProvider == cmd.pgProvider()
                        && currency.equals(cmd.currency())
                        && couponId.equals(cmd.couponId())
                        && pointUsageId.equals(cmd.pointUsageId());
    }
}

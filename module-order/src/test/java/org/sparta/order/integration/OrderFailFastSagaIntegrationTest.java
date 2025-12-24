package org.sparta.order.integration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sparta.common.event.DomainEvent;
import org.sparta.common.event.EventPublisher;
import org.sparta.common.event.order.OrderCancelledEvent;
import org.sparta.order.application.command.OrderCommand;
import org.sparta.order.application.dto.CouponReservationResult;
import org.sparta.order.application.dto.PointReservationResult;
import org.sparta.order.application.dto.StockReservationResult;
import org.sparta.order.application.error.ServiceUnavailableException;
import org.sparta.order.application.service.IdempotencyService;
import org.sparta.order.application.service.OrderService;
import org.sparta.order.application.service.reservation.CouponReservationService;
import org.sparta.order.application.service.reservation.PaymentApprovalService;
import org.sparta.order.application.service.reservation.PointReservationService;
import org.sparta.order.application.service.reservation.StockReservationService;
import org.sparta.order.domain.circuitbreaker.CircuitBreaker;
import org.sparta.order.domain.entity.Order;
import org.sparta.order.domain.entity.OrderOutboxEvent;
import org.sparta.order.domain.repository.OrderOutboxEventRepository;
import org.sparta.order.domain.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Disabled
@SpringBootTest
@ActiveProfiles("test")
// Fail Fast 가드와 Saga 보상 이벤트가 Spring 통합 환경에서 제대로 동작하는지 확인하는 통합 테스트
class OrderFailFastSagaIntegrationTest {

    @Autowired
    private OrderService orderService;

    @MockitoBean
    private OrderRepository orderRepository;

    @MockitoBean
    private OrderOutboxEventRepository orderOutboxEventRepository;

    @MockitoBean
    private EventPublisher eventPublisher;

    @MockitoBean
    private IdempotencyService idempotencyService;

    @MockitoBean
    private StockReservationService stockReservationService;

    @MockitoBean
    private PointReservationService pointReservationService;

    @MockitoBean
    private CouponReservationService couponReservationService;

    @MockitoBean
    private PaymentApprovalService paymentApprovalService;

    @MockitoBean
    private CircuitBreaker circuitBreaker;

    @Test
    @DisplayName("Circuit Breaker OPEN 상태면 주문 생성이 통과하지 못한다")
    void createOrder_failsFastWhenBreakerOpen() {
        // given
        when(circuitBreaker.isOpen("stock-service")).thenReturn(true);

        OrderCommand.Create command = sampleCreateCommand();

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(UUID.randomUUID(), command))
            .isInstanceOf(ServiceUnavailableException.class);

        verifyNoInteractions(orderRepository, stockReservationService);
    }

    @Test
    @DisplayName("결제 단계에서 실패하면 Saga가 OrderCancelledEvent를 발행한다")
    void sagaCompensatesWhenPaymentFails() {
        // given
        UUID customerId = UUID.randomUUID();
        OrderCommand.Create command = sampleCreateCommand();

        mockOrderSave();
        when(orderOutboxEventRepository.save(any(OrderOutboxEvent.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        when(circuitBreaker.isOpen(anyString())).thenReturn(false);

        StockReservationResult stockResult =
            new StockReservationResult(UUID.randomUUID(), command.quantity(), "RESERVED");
        when(stockReservationService.reserve(any(), anyString(), anyInt())).thenReturn(stockResult);

        PointReservationResult pointResult =
            new PointReservationResult("point-res", 0L, "RESERVED");
        when(pointReservationService.reserve(any(), any(), anyLong(), anyLong()))
            .thenReturn(pointResult);

        CouponReservationResult couponResult =
            new CouponReservationResult(UUID.randomUUID(), 0L, true);
        when(couponReservationService.reserve(any(), any(), any(), anyLong()))
            .thenReturn(couponResult);

        when(paymentApprovalService.approve(
            any(), any(), anyLong(), anyString(), anyString(), anyString()
        )).thenThrow(new RuntimeException("PG down"));

        // when
        assertThatThrownBy(() -> orderService.createOrder(customerId, command))
            .isInstanceOf(RuntimeException.class);

        // then
        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher).publishExternal(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(OrderCancelledEvent.class);
    }

    private void mockOrderSave() {
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            try {
                Field idField = Order.class.getDeclaredField("id");
                idField.setAccessible(true);
                if (idField.get(order) == null) {
                    idField.set(order, UUID.randomUUID());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return order;
        });
    }

    private OrderCommand.Create sampleCreateCommand() {
        return new OrderCommand.Create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            2,
            15_000,
            "서울시 송파구 1-1",
            "홍길동",
            "010-3333-4444",
            "slack-user",
            LocalDateTime.now().plusDays(2),
            "삼가 요청",
            0,
            "CARD",
            "TOSS",
            "KRW",
            UUID.randomUUID()
        );
    }
}

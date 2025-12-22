package org.sparta.order.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sparta.common.event.DomainEvent;
import org.sparta.common.event.EventPublisher;
import org.sparta.common.event.order.OrderCancelledEvent;
import org.sparta.order.application.command.OrderCommand;
import org.sparta.order.application.dto.CouponReservationResult;
import org.sparta.order.application.dto.PaymentApprovalResult;
import org.sparta.order.application.dto.PointReservationResult;
import org.sparta.order.application.dto.StockReservationResult;
import org.sparta.order.application.error.ServiceUnavailableException;
import org.sparta.order.application.service.reservation.CouponReservationService;
import org.sparta.order.application.service.reservation.PaymentApprovalService;
import org.sparta.order.application.service.reservation.PointReservationService;
import org.sparta.order.application.service.reservation.StockReservationService;
import org.sparta.order.domain.circuitbreaker.CircuitBreaker;
import org.sparta.order.domain.entity.Order;
import org.sparta.order.domain.entity.OrderOutboxEvent;
import org.sparta.order.domain.error.OrderErrorType;
import org.sparta.order.domain.repository.OrderOutboxEventRepository;
import org.sparta.order.domain.repository.OrderRepository;
import org.sparta.order.presentation.dto.response.OrderResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
// OrderService의 성공/실패 분기와 보상 이벤트 발행 여부를  검증하는 단위 시나리오 테스트
class OrderCreateScenarioTest {

    @Autowired
    private OrderService orderService;

    @MockitoBean
    private OrderRepository orderRepository;

    @MockitoBean
    private OrderOutboxEventRepository orderOutboxEventRepository;

    @MockitoBean
    private EventPublisher eventPublisher;

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
    @DisplayName("주문 생성 성공 - 모든 외부 의존성이 정상 처리된다")
    void createOrder_success() {
        UUID customerId = UUID.randomUUID();
        OrderCommand.Create command = createCommand(100, true);

        mockOrderSave();
        when(orderOutboxEventRepository.save(any(OrderOutboxEvent.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(circuitBreaker.isOpen(anyString())).thenReturn(false);

        when(stockReservationService.reserve(any(), anyString(), anyInt()))
            .thenReturn(new StockReservationResult(UUID.randomUUID(), command.quantity(), "RESERVED"));

        when(pointReservationService.reserve(any(), any(), anyLong(), anyLong()))
            .thenReturn(new PointReservationResult(UUID.randomUUID().toString(), 50L, "RESERVED"));

        when(couponReservationService.reserve(any(), any(), any(), anyLong()))
            .thenReturn(new CouponReservationResult(UUID.randomUUID(), 500L, true));

        when(paymentApprovalService.approve(any(), any(), anyLong(), anyString(), anyString(), anyString()))
            .thenReturn(new PaymentApprovalResult(UUID.randomUUID(), "pay-key", true, LocalDateTime.now().toString()));

        OrderResponse.Detail response = orderService.createOrder(customerId, command);

        assertThat(response.orderId()).isNotNull();
        verify(orderOutboxEventRepository, times(1)).save(any(OrderOutboxEvent.class));
        verifyNoInteractions(eventPublisher); // Outbox 패턴만 사용
    }

    @Test
    @DisplayName("Circuit Breaker가 OPEN이면 주문이 즉시 실패한다")
    void createOrder_failFastWhenCircuitOpen() {
        when(circuitBreaker.isOpen("stock-service")).thenReturn(true);

        assertThatThrownBy(() -> orderService.createOrder(UUID.randomUUID(), createCommand(0, false)))
            .isInstanceOf(ServiceUnavailableException.class);

        verifyNoInteractions(orderRepository, stockReservationService);
    }

    @Test
    @DisplayName("재고 예약 실패 시 주문 생성이 중단된다")
    void createOrder_stockFailure() {
        mockHappyPathStubs();
        when(stockReservationService.reserve(any(), anyString(), anyInt()))
            .thenThrow(new org.sparta.common.error.BusinessException(OrderErrorType.STOCK_RESERVATION_FAILED));

        assertThatThrownBy(() -> orderService.createOrder(UUID.randomUUID(), createCommand(0, false)))
            .isInstanceOf(org.sparta.common.error.BusinessException.class)
            .hasMessageContaining(OrderErrorType.STOCK_RESERVATION_FAILED.getMessage());

        verifyNoInteractions(pointReservationService, couponReservationService, paymentApprovalService);
        captureCancelledEvent();
    }

    @Test
    @DisplayName("포인트 예약 실패 시 주문 생성이 중단된다")
    void createOrder_pointFailure() {
        mockHappyPathStubs();
        when(pointReservationService.reserve(any(), any(), anyLong(), anyLong()))
            .thenThrow(new org.sparta.common.error.BusinessException(OrderErrorType.POINT_RESERVATION_FAILED));

        assertThatThrownBy(() -> orderService.createOrder(UUID.randomUUID(), createCommand(100, false)))
            .isInstanceOf(org.sparta.common.error.BusinessException.class)
            .hasMessageContaining(OrderErrorType.POINT_RESERVATION_FAILED.getMessage());

        verifyNoInteractions(couponReservationService, paymentApprovalService);
        captureCancelledEvent();
    }

    @Test
    @DisplayName("쿠폰 예약 실패 시 주문 생성이 중단된다")
    void createOrder_couponFailure() {
        mockHappyPathStubs();
        when(couponReservationService.reserve(any(), any(), any(), anyLong()))
            .thenThrow(new org.sparta.common.error.BusinessException(OrderErrorType.COUPON_RESERVATION_FAILED));

        assertThatThrownBy(() -> orderService.createOrder(UUID.randomUUID(), createCommand(0, true)))
            .isInstanceOf(org.sparta.common.error.BusinessException.class)
            .hasMessageContaining(OrderErrorType.COUPON_RESERVATION_FAILED.getMessage());

        verifyNoInteractions(paymentApprovalService);
        captureCancelledEvent();
    }

    @Test
    @DisplayName("결제 승인 실패 시 OrderCancelledEvent가 발행된다")
    void createOrder_paymentFailureTriggersSaga() {
        mockHappyPathStubs();
        when(paymentApprovalService.approve(any(), any(), anyLong(), anyString(), anyString(), anyString()))
            .thenThrow(new RuntimeException("PG down"));

        assertThatThrownBy(() -> orderService.createOrder(UUID.randomUUID(), createCommand(0, false)))
            .isInstanceOf(RuntimeException.class);

        captureCancelledEvent();
    }

    private void mockHappyPathStubs() {
        mockOrderSave();
        when(orderOutboxEventRepository.save(any(OrderOutboxEvent.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(circuitBreaker.isOpen(anyString())).thenReturn(false);
        when(stockReservationService.reserve(any(), anyString(), anyInt()))
            .thenReturn(new StockReservationResult(UUID.randomUUID(), 1, "RESERVED"));
        when(pointReservationService.reserve(any(), any(), anyLong(), anyLong()))
            .thenReturn(new PointReservationResult(UUID.randomUUID().toString(), 0L, "RESERVED"));
        when(couponReservationService.reserve(any(), any(), any(), anyLong()))
            .thenReturn(new CouponReservationResult(UUID.randomUUID(), 0L, true));
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

    private OrderCommand.Create createCommand(int requestPoint, boolean useCoupon) {
        return new OrderCommand.Create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            2,
            20_000,
            "서울시 강남구 어딘가 1-1",
            "홍길동",
            "010-0000-0000",
            "slack-user",
            LocalDateTime.now().plusDays(1),
            "빠른 배송 요청",
            requestPoint,
            "CARD",
            "TOSS",
            "KRW",
            useCoupon ? UUID.randomUUID() : null
        );
    }

    private OrderCancelledEvent captureCancelledEvent() {
        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher).publishExternal(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(OrderCancelledEvent.class);
        return (OrderCancelledEvent) captor.getValue();
    }
}

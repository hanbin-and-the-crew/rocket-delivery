package org.sparta.order.application.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sparta.common.event.DomainEvent;
import org.sparta.common.event.EventPublisher;
import org.sparta.common.event.order.OrderCancelledEvent;
import org.sparta.order.application.command.OrderCommand;
import org.sparta.order.application.error.ServiceUnavailableException;
import org.sparta.order.domain.entity.Order;
import org.sparta.order.domain.enumeration.CanceledReasonCode;
import org.sparta.order.domain.repository.IdempotencyRepository;
import org.sparta.order.domain.repository.OrderRepository;
import org.sparta.order.infrastructure.client.CouponClient;
import org.sparta.order.infrastructure.client.PaymentClient;
import org.sparta.order.infrastructure.client.PointClient;
import org.sparta.order.infrastructure.client.StockClient;
import org.sparta.order.domain.circuitbreaker.CircuitBreaker;
import org.sparta.order.presentation.dto.response.OrderResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@Disabled
@SpringBootTest
@ActiveProfiles("test")
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @MockitoBean
    private OrderRepository orderRepository;

    @MockitoBean
    private EventPublisher eventPublisher;

    @MockitoBean
    private IdempotencyRepository idempotencyRepository;

    // ObjectMapper는 Mock하지 않고 실제 Bean 사용
    // @MockitoBean 제거!

    // ===== Feign Client Mock 선언 =====
    @MockitoBean
    private StockClient stockClient;

    @MockitoBean
    private PointClient pointClient;

    @MockitoBean
    private CouponClient couponClient;

    @MockitoBean
    private PaymentClient paymentClient;

    @MockitoBean
    private CircuitBreaker circuitBreaker;
    // ===================================

    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();


    @Test
    @DisplayName("주문 취소 시 OrderCancelledEvent가 발행된다")
    void cancelOrder_publishesOrderCancelledEvent() {
        // given
        Order existing = createExistingOrderWithId(ORDER_ID);
        when(orderRepository.findByIdAndDeletedAtIsNull(ORDER_ID))
                .thenReturn(Optional.of(existing));

        OrderCommand.Cancel request = new OrderCommand.Cancel(
                ORDER_ID,
                CanceledReasonCode.CUSTOMER_REQUEST.name(),
                "고객 요청 취소"
        );

        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);

        // when
        OrderResponse.Update response = orderService.cancelOrder(request);

        // then
        assertThat(response.orderId()).isEqualTo(ORDER_ID);
        assertThat(response.message()).contains("취소");

        verify(eventPublisher, times(1)).publishExternal(eventCaptor.capture());
        DomainEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent).isInstanceOf(OrderCancelledEvent.class);

        OrderCancelledEvent cancelledEvent = (OrderCancelledEvent) publishedEvent;
        assertThat(cancelledEvent.orderId()).isEqualTo(ORDER_ID);
    }

    @Test
    @DisplayName("Circuit Breaker가 OPEN이면 주문 생성이 즉시 실패한다")
    void createOrder_failFastWhenCircuitBreakerOpen() {
        // given
        when(circuitBreaker.isOpen("stock-service")).thenReturn(true);

        OrderCommand.Create command = createSampleCreateCommand();

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(UUID.randomUUID(), command))
            .isInstanceOf(ServiceUnavailableException.class);

        verifyNoInteractions(orderRepository);
    }

    private Order createExistingOrderWithId(UUID id) {
        Order order = Order.create(
                CUSTOMER_ID,
                UUID.randomUUID(), // supplierCompanyId
                UUID.randomUUID(), // supplierHubId
                UUID.randomUUID(), // receiptCompanyId
                UUID.randomUUID(), // receiptHubId
                UUID.randomUUID(), // productId
                10_000L,
                2,
                LocalDateTime.now().plusDays(1),
                "서울시 어딘가 1-1",
                "요청사항입니다",
                "홍길동",
                "010-0000-0000",
                "slack@example.com"
        );
        try {
            java.lang.reflect.Field idField = Order.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(order, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return order;
    }

    private OrderCommand.Create createSampleCreateCommand() {
        return new OrderCommand.Create(
                UUID.randomUUID(), // supplierCompanyId
                UUID.randomUUID(), // supplierHubId
                UUID.randomUUID(), // receiptCompanyId
                UUID.randomUUID(), // receiptHubId
                UUID.randomUUID(), // productId
                1,
                10_000,
                "서울시 강남구 1-1",
                "홍길동",
                "010-1111-2222",
                "slack-user",
                LocalDateTime.now().plusDays(1),
                "빠른 배송 부탁",
                0,
                "CARD",
                "TOSS",
                "KRW",
                null
        );
    }
}

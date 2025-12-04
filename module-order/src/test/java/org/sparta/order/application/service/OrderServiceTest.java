package org.sparta.order.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sparta.common.event.DomainEvent;
import org.sparta.common.event.EventPublisher;
import org.sparta.order.application.command.OrderCommand;
import org.sparta.order.presentation.dto.OrderMapper;
import org.sparta.order.presentation.dto.request.OrderRequest;
import org.sparta.order.presentation.dto.response.OrderResponse;
import org.sparta.order.domain.entity.Order;
import org.sparta.order.domain.enumeration.CanceledReasonCode;
import org.sparta.order.domain.repository.OrderRepository;
import org.sparta.order.infrastructure.event.publisher.OrderCancelledEvent;
import org.sparta.order.infrastructure.event.publisher.OrderCreatedEvent;
import org.sparta.order.OrderApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = OrderApplication.class)
@ActiveProfiles("test")
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderMapper orderMapper;

    @MockitoBean
    private OrderRepository orderRepository;

    @MockitoBean
    private EventPublisher eventPublisher;

    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();

    @Test
    @DisplayName("주문 생성 시 OrderCreatedEvent가 발행된다")
    void createOrder_publishesOrderCreatedEvent() {
        // given
        OrderRequest.Create request = new OrderRequest.Create(
                UUID.randomUUID(), // supplierCompanyId
                UUID.randomUUID(), // supplierHubId
                UUID.randomUUID(), // receiptCompanyId
                UUID.randomUUID(), // receiptHubId
                UUID.randomUUID(), // productId
                3,                 // quantity
                10_000,            // productPrice
                "서울시 어딘가 1-1",
                "홍길동",
                "010-0000-0000",
                "slack@example.com",
                LocalDateTime.now().plusDays(1),
                "요청 메모"
        );

        // request DTO -> command 변환 작업 필요
        OrderCommand.Create command = orderMapper.toCommand(request);


        // Repository.save() 가 호출되면 Order에 id를 채워서 반환되도록 세팅
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            // id가 null일 수 있으니 강제로 세팅
            java.lang.reflect.Field idField = Order.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(o, ORDER_ID);
            return o;
        });

        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);

        // when
        OrderResponse.Detail response = orderService.createOrder(CUSTOMER_ID, command);

        // then
        assertThat(response.orderId()).isEqualTo(ORDER_ID);

        verify(orderRepository, times(1)).save(any(Order.class));
        verify(eventPublisher, times(1)).publishExternal(eventCaptor.capture());

        DomainEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent).isInstanceOf(OrderCreatedEvent.class);
        OrderCreatedEvent createdEvent = (OrderCreatedEvent) publishedEvent;
        assertThat(createdEvent.orderId()).isEqualTo(ORDER_ID);
        assertThat(createdEvent.quantity()).isEqualTo(request.quantity());
    }

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
}

package org.sparta.order.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sparta.common.event.EventPublisher;
import org.sparta.order.application.dto.request.OrderRequest;
import org.sparta.order.application.dto.response.OrderResponse;
import org.sparta.order.application.service.OrderService;
import org.sparta.order.domain.entity.Order;
import org.sparta.order.domain.enumeration.OrderStatus;
import org.sparta.order.domain.repository.OrderRepository;
import org.sparta.order.domain.vo.Money;
import org.sparta.order.domain.vo.Quantity;
import org.sparta.order.infrastructure.client.ProductClient;
import org.sparta.order.infrastructure.client.UserClient;
import org.sparta.order.infrastructure.event.Listener.OrderEventListener;
import org.sparta.order.infrastructure.event.dto.OrderCreatedEvent;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.any;


/**
 * 3주차 이벤트 리스너 기반 이벤트 처리
 * Step1. Spring Event 기초와 이벤트 발행
 * createOrder이 정상적으로 실행되면 테스트도 통과할 것임.. (현재 다른 도메인이랑 주고받는 부분에서 문제)
 */
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public class OrderServiceEventTest {

    @InjectMocks
    private OrderService orderService;

    @Mock private EventPublisher springOrderEventPublisher;
    @Mock private OrderEventListener orderEventListener;
    @Mock private UserClient userClient;
    @Mock private ProductClient productClient;
    @Mock private OrderRepository orderRepository;

    @Test
    @DisplayName("주문 생성 시 OrderCreatedEvent가 발행된다")
    void createOrder_ShouldPublishOrderCreatedEvent() {

        // given
        UUID userId = java.util.UUID.randomUUID();

        OrderRequest.Create request = new OrderRequest.Create(
                UUID.randomUUID(), // supplierId
                UUID.randomUUID(), // supplierCompanyId
                UUID.randomUUID(), // supplierHubId
                UUID.randomUUID(), // receiptCompanyId
                UUID.randomUUID(), // receiptHubId
                UUID.randomUUID(), // productId
                5, // quantity
                "서울특별시 강남구 테헤란로 123", // deliveryAddress
                "최원철", // userName
                "010-1111-2222", // userPhoneNumber
                "12@1234.com", // slackId
                LocalDateTime.now().plusDays(7), // dueAt
                "빠른 배송 부탁드립니다" // requestedMemo
        );

        // 상품 스냅샷 (테스트용 mock 데이터)
        var product = new Object() {
            String name() { return "아메리카노 원두 1kg"; }
            long price() { return 12000L; }
        };

        // when
        orderService.createOrder(request, userId);

        // then
        // 이벤트 리스너가 호출되었는지 검증
        verify(orderEventListener, times(1))
                .handleOrderCreated(any(OrderCreatedEvent.class));
    }
}

package org.sparta.order.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.order.application.dto.request.OrderRequest;
import org.sparta.order.application.service.OrderService;
import org.sparta.order.application.event.PaymentEventListener;
import org.sparta.order.infrastructure.event.publisher.OrderCreatedSpringEvent;
import org.sparta.order.infrastructure.repository.OrderJpaRepository;
import org.sparta.order.support.fixtures.OrderFixture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;


/**
 * 3주차 이벤트 리스너 기반 이벤트 처리
 * Step2. TransactionalEventListener와 트랜잭션 안전
 * createOrder이 정상적으로 실행되면 테스트도 통과할 것임.. (현재 다른 도메인이랑 주고받는 부분에서 문제)
 */
@SpringBootTest
class TransactionalEventTest {

    private static final UUID userId = UUID.fromString("10000000-0000-0000-0000-000000000001");

    @Autowired
    private OrderService orderService;

    @MockitoSpyBean
    private PaymentEventListener paymentEventListener;

    @Autowired
    private OrderJpaRepository orderRepository;

    // DB 매번 비우는 행위
    @Autowired
    private PlatformTransactionManager txManager;

    @BeforeEach
    void clearDatabase() {
        TransactionTemplate tx = new TransactionTemplate(txManager);
        tx.execute(status -> {
            orderRepository.deleteAll();
            return null;
        });
    }
/*
    @Test
    @DisplayName("트랜잭션 롤백 시 이벤트가 발행되지 않는다")
    void whenTransactionRollback_EventShouldNotBePublished() {

        // given
        OrderRequest.Create request = OrderFixture.createInvalidRequest(); // 예외를 발생시킬 요청

        // when & then
        // 주문 생성 중 예외가 발생하여 트랜잭션이 롤백됩니다.
        assertThatThrownBy(() -> orderService.createOrder(request, userId))
                .isInstanceOf(IllegalArgumentException.class);

        // 트랜잭션이 롤백되었으므로, 주문도 저장되지 않고
        assertThat(orderRepository.findAll()).isEmpty();

        // 이벤트 리스너도 실행되지 않습니다.
        verify(paymentEventListener, never())
                .handleOrderCreated(any(OrderCreatedSpringEvent.class));
    }*/

/*    @Test
    @DisplayName("트랜잭션 커밋 성공 시 이벤트가 발행된다")
    void whenTransactionCommit_EventShouldBePublished() {

        // given
        OrderRequest.Create request = OrderFixture.createValidRequest();

        // when
        orderService.createOrder(request, userId);

        // then
        // 트랜잭션이 성공적으로 커밋되었으므로
        assertThat(orderRepository.findAll()).hasSize(1);

        // 이벤트 리스너도 실행됩니다.
        verify(paymentEventListener, timeout(1000).times(1))
                .handleOrderCreated(any(OrderCreatedSpringEvent.class));
    }*/
}
//package org.sparta.order.application;
//
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.sparta.order.application.dto.request.OrderRequest;
//import org.sparta.order.application.service.OrderService;
//import org.sparta.order.application.event.PaymentEventListener;
//import org.sparta.order.infrastructure.event.publisher.OrderCreatedSpringEvent;
//import org.sparta.order.support.fixtures.OrderFixture;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
//
//import java.util.UUID;
//
//import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
///**
// * 3주차 이벤트 리스너 기반 이벤트 처리
// * Step3. 비동기 처리와 ThreadPool 설정
// * createOrder이 정상적으로 실행되면 테스트도 통과할 것임.. (현재 다른 도메인이랑 주고받는 부분에서 문제)
// */
//@Slf4j
//@SpringBootTest
//class AsyncEventTest {
//
//    private static final UUID userId = UUID.fromString("10000000-0000-0000-0000-000000000001");
//
//    @Autowired
//    private OrderService orderService;
//
//    @MockitoSpyBean
//    private PaymentEventListener paymentEventListener;
///*
//
//    @Test
//    @DisplayName("이벤트가 비동기로 처리된다")
//    void events_ShouldBeProcessedAsynchronously() throws InterruptedException {
//
//        // given
//        OrderRequest.Create request = OrderFixture.createValidRequest();
//        String mainThread = Thread.currentThread().getName();
//
//        log.info("메인 스레드: {}", mainThread);
//
//        // when
//        orderService.createOrder(request, userId);
//
//        // 비동기 처리를 위한 대기
//        Thread.sleep(1000);
//
//        // then
//        verify(paymentEventListener, times(1))
//                .handleOrderCreatedAsync(any(OrderCreatedSpringEvent.class));
//
//        // 로그를 확인하면 "order-async-" 접두사가 붙은 스레드 이름을 볼 수 있습니다.
//        // 이를 통해 비동기로 실행되었음을 확인할 수 있습니다.
//    }
//*/
//
///*    @Test
//    @DisplayName("비동기 처리 중 예외가 발생해도 메인 로직에 영향을 주지 않는다")
//    void whenAsyncEventThrowsException_MainFlowShouldNotBeAffected() {
//
//        // given
//        OrderRequest.Create request = OrderFixture.createValidRequest();
//
//        // Mock을 설정하여 결제 처리 중 예외가 발생하도록 합니다.
//        doThrow(new RuntimeException("결제 실패"))
//                .when(paymentEventListener)
//                .handleOrderCreatedAsync(any(OrderCreatedSpringEvent.class));
//
//        // when & then
//        // 비동기 작업에서 예외가 발생해도, 메인 로직(주문 생성)은 성공합니다.
//        assertThatCode(() -> orderService.createOrder(request, userId))
//                .doesNotThrowAnyException();
//    }*/
//}

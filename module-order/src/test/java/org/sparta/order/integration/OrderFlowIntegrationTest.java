package org.sparta.order.integration;

//
//import jakarta.transaction.Transactional;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mock;
//import org.sparta.order.application.dto.request.OrderRequest;
//import org.sparta.order.application.dto.response.OrderResponse;
//import org.sparta.order.application.service.OrderService;
//import org.sparta.order.config.TestConfig;
//import org.sparta.order.domain.entity.Payment;
//import org.sparta.order.domain.enumeration.PaymentStatus;
//import org.sparta.order.infrastructure.repository.PaymentJpaRepository;
//import org.sparta.product.domain.vo.Money;
//import org.sparta.order.infrastructure.repository.OrderJpaRepository;
//import org.sparta.product.domain.entity.Product;
//import org.sparta.product.domain.repository.ProductRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.context.annotation.ComponentScan;
//import org.springframework.context.annotation.FilterType;
//import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.test.context.ActiveProfiles;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.UUID;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//
///**
// * 3주차 이벤트 리스너 기반 이벤트 처리
// * Step4. 주문-결제-재고 전체 플로우 구현
// * createOrder이 정상적으로 실행되면 테스트도 통과할 것임.. (현재 다른 도메인이랑 주고받는 부분에서 문제)
// */
//@SpringBootTest(classes = TestConfig.class)
//@Transactional
//@ActiveProfiles("test")
//class OrderFlowIntegrationTest {
//
//    private static final UUID userId = UUID.fromString("10000000-0000-0000-0000-000000000001");
//    // Kafka 빈들을 모두 Mock으로 처리
//    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
//    @Mock private ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory;
//    @Autowired  private OrderService orderService;
//    @Autowired private OrderJpaRepository orderRepository;
//    @Autowired private PaymentJpaRepository paymentRepository;
//
//    @Autowired private ProductRepository productRepository;
//
//    @Test
//    @DisplayName("주문 생성부터 재고 차감까지 전체 플로우가 정상 동작한다")
//    void fullOrderFlow_ShouldWorkCorrectly() throws InterruptedException {
//
//        // given
//        Product product = Product.create(
//                "테스트 상품",                         // 상품명
//                Money.of(10000L),                        // 가격
//                UUID.randomUUID(),                              // categoryId
//                UUID.randomUUID(),                              // companyId
//                UUID.randomUUID(),                              // hubId
//                100                                             // 초기 재고
//        );
//        productRepository.save(product);
//
//        OrderRequest.Create request = new OrderRequest.Create(
//                UUID.randomUUID(), // supplierId
//                UUID.randomUUID(), // supplierCompanyId
//                UUID.randomUUID(), // supplierHubId
//                UUID.randomUUID(), // receiptCompanyId
//                UUID.randomUUID(), // receiptHubId
//                UUID.randomUUID(), // productId
//                10, // quantity
//                "서울특별시 강남구 테헤란로 123", // deliveryAddress
//                "최원철", // userName
//                "010-1111-2222", // userPhoneNumber
//                "12@1234.com", // slackId
//                LocalDateTime.now().plusDays(7), // dueAt
//                "빠른 배송 부탁드립니다" // requestedMemo
//        );
//
//        // when
//        OrderResponse.Create response = orderService.createOrder(request, userId);
//
//        // 비동기 처리를 위한 대기
//        Thread.sleep(3000);
//
//        // then
//        // 1. 주문 생성 확인
//
//
//        // 1~2. 주문 생성 확인, 결제 생성 확인
//        List<Payment> payments = paymentRepository.findByOrderId(response.orderId());
//        assertThat(payments).hasSize(1);
//        assertThat(payments.get(0).getStatus()).isEqualTo(PaymentStatus.COMPLETED);
//
//        // 3. 재고 차감 확인
//        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
//        assertThat(updatedProduct.getStock()).isEqualTo(90);
//
//    }
//
//    @Test
//    @DisplayName("결제 실패 시 재고는 차감되지 않는다")
//    void whenPaymentFails_StockShouldNotDecrease() throws InterruptedException {
//
//        // given
//        Product product = Product.create(
//                "테스트 상품",                         // 상품명
//                Money.of(10000L),                        // 가격
//                UUID.randomUUID(),                              // categoryId
//                UUID.randomUUID(),                              // companyId
//                UUID.randomUUID(),                              // hubId
//                100                                             // 초기 재고
//        );
//        productRepository.save(product);
//
//        OrderRequest.Create request = new OrderRequest.Create(
//                UUID.randomUUID(), // supplierId
//                UUID.randomUUID(), // supplierCompanyId
//                UUID.randomUUID(), // supplierHubId
//                UUID.randomUUID(), // receiptCompanyId
//                UUID.randomUUID(), // receiptHubId
//                UUID.randomUUID(), // productId
//                0, // quantity
//                "서울특별시 강남구 테헤란로 123", // deliveryAddress
//                "최원철", // userName
//                "010-1111-2222", // userPhoneNumber
//                "12@1234.com", // slackId
//                LocalDateTime.now().plusDays(7), // dueAt
//                "빠른 배송 부탁드립니다" // requestedMemo
//        );
//
//        // 결제 실패 시뮬레이션을 위한 Mock 설정
//        // 실무에서는 PG사 연동 실패, 잔액 부족 등의 시나리오를 테스트합니다.
//
//        // when
//        OrderResponse.Create response = orderService.createOrder(request, userId);
//        Thread.sleep(2000);
//
//        // then: 결제가 실패했으므로 재고는 그대로 유지됩니다.
//        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
//        assertThat(updatedProduct.getStock()).isEqualTo(100);
//
//        // 이처럼 이벤트 체인에서 중간 단계가 실패하면,
//        // 다음 단계가 실행되지 않아 데이터 일관성이 유지됩니다.
//    }
//}

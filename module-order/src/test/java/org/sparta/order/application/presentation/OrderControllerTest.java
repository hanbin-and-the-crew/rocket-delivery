//package org.sparta.order.presentation;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.sparta.common.event.EventPublisher;
//import org.sparta.order.OrderApplication;
//import org.sparta.order.application.dto.request.OrderRequest;
//import org.sparta.order.application.dto.response.OrderResponse;
//import org.sparta.order.domain.entity.Order;
//import org.sparta.order.domain.enumeration.OrderStatus;
//import org.sparta.order.domain.repository.OrderRepository;
//import org.sparta.order.infrastructure.event.publisher.OrderCancelledEvent;
//import org.sparta.order.infrastructure.event.publisher.OrderCreatedEvent;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//
//import java.time.LocalDateTime;
//import java.util.UUID;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.times;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
///**
// * Order 모듈 통합 테스트
// * - 실제 Controller + Service + Repository 사용
// * - EventPublisher만 MockitoBean으로 교체 (Kafka 호출 방지)
// */
//@SpringBootTest(
//        classes = OrderApplication.class,
//        properties = {
//                "eureka.client.enabled=false",            // 테스트 시 Eureka 호출 방지
//                "spring.jpa.hibernate.ddl-auto=none"      // 실제 DDL 전략은 프로젝트 설정에 맞게 조정
//        }
//)
//@AutoConfigureMockMvc
//@ActiveProfiles("test")
//class OrderControllerTest {
//
//    @Autowired
//    MockMvc mockMvc;
//
//    @Autowired
//    ObjectMapper objectMapper;
//
//    @Autowired
//    OrderRepository orderRepository;
//
//    @MockitoBean
//    EventPublisher eventPublisher;
//
//    /**
//     * 주문 생성 통합 테스트
//     * - POST /api/orders
//     * - X-USER-ID 헤더 사용
//     * - EventPublisher.publishExternal(OrderCreatedEvent) 호출 검증
//     */
//    @Test
//    @DisplayName("주문 생성 API - 성공 시 DB에 저장되고 OrderCreatedEvent가 발행된다")
//    void createOrder_success() throws Exception {
//        // given
//        UUID customerId = UUID.randomUUID();
//
//        OrderRequest.Create requestDto = new OrderRequest.Create(
//                UUID.randomUUID(), // supplierCompanyId
//                UUID.randomUUID(), // supplierHubId
//                UUID.randomUUID(), // receiptCompanyId
//                UUID.randomUUID(), // receiptHubId
//                UUID.randomUUID(), // productId
//                3,                 // quantity
//                10_000,            // productPrice
//                "서울특별시 강남구 테헤란로 123",
//                "홍길동",
//                "010-1111-2222",
//                "slack@example.com",
//                LocalDateTime.now().plusDays(1),
//                "빠른 배송 부탁드립니다"
//        );
//
//        String body = objectMapper.writeValueAsString(requestDto);
//
//        // when
//        String responseJson = mockMvc.perform(
//                        post("/api/orders")
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .header("X-USER-ID", customerId.toString())
//                                .content(body)
//                )
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
//                .andExpect(jsonPath("$.data.orderId").exists())
//                .andExpect(jsonPath("$.data.orderStatus").value("CREATED"))
//                .andReturn()
//                .getResponse()
//                .getContentAsString();
//
//        // then
//        JsonNode root = objectMapper.readTree(responseJson);
//        String orderIdStr = root.path("data").path("orderId").asText();
//        UUID orderId = UUID.fromString(orderIdStr);
//
//        // DB에 저장되었는지 확인
//        Order saved = orderRepository.findByIdAndDeletedAtIsNull(orderId)
//                .orElseThrow(() -> new IllegalStateException("주문이 저장되지 않았습니다."));
//
//        assertThat(saved.getCustomerId()).isEqualTo(customerId);
//        assertThat(saved.getOrderStatus()).isEqualTo(OrderStatus.CREATED);
//
//        // 이벤트 발행 검증
//        verify(eventPublisher, times(1))
//                .publishExternal(any(OrderCreatedEvent.class));
//    }
//
//    /**
//     * 주문 단건 조회 통합 테스트
//     * - GET /api/orders/{orderId}
//     */
//    @Test
//    @DisplayName("주문 단건 조회 API - 저장된 주문을 조회할 수 있다")
//    void getOrder_success() throws Exception {
//        // given: DB에 미리 하나 저장
//        Order order = createAndSaveOrder();
//
//        // when & then
//        mockMvc.perform(
//                        get("/api/orders/{orderId}", order.getId())
//                )
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
//                .andExpect(jsonPath("$.data.orderId").value(order.getId().toString()))
//                .andExpect(jsonPath("$.data.orderStatus").value(order.getOrderStatus().name()));
//    }
//
//    /**
//     * 주문 취소 통합 테스트
//     * - POST /api/orders/{orderId}/cancel
//     * - EventPublisher.publishExternal(OrderCancelledEvent) 호출 검증
//     */
//    @Test
//    @DisplayName("주문 취소 API - CREATED/APPROVED 상태의 주문은 취소되고 이벤트가 발행된다")
//    void cancelOrder_success() throws Exception {
//        // given
//        Order order = createAndSaveOrder(); // 기본 CREATED 상태
//
//        OrderRequest.Cancel cancelRequest = new OrderRequest.Cancel(
//                order.getId(),
//                "CUSTOMER_REQUEST",
//                "고객 요청으로 취소합니다"
//        );
//
//        String body = objectMapper.writeValueAsString(cancelRequest);
//
//        // when
//        String responseJson = mockMvc.perform(
//                        post("/api/orders/{orderId}/cancel", order.getId())
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .content(body)
//                )
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
//                .andExpect(jsonPath("$.data.orderId").value(order.getId().toString()))
//                .andExpect(jsonPath("$.data.message").value("주문이 취소되었습니다."))
//                .andReturn()
//                .getResponse()
//                .getContentAsString();
//
//        // then: DB 상태 확인
//        Order cancelled = orderRepository.findByIdAndDeletedAtIsNull(order.getId())
//                .orElseThrow(() -> new IllegalStateException("주문이 존재하지 않습니다."));
//
//        assertThat(cancelled.getOrderStatus()).isEqualTo(OrderStatus.CANCELED);
//        assertThat(cancelled.getCanceledReasonCode()).isNotNull();
//        assertThat(cancelled.getCanceledReasonMemo()).isEqualTo("고객 요청으로 취소합니다");
//        assertThat(cancelled.getCanceledAt()).isNotNull();
//
//        // 이벤트 발행 검증
//        verify(eventPublisher, times(1))
//                .publishExternal(any(OrderCancelledEvent.class));
//    }
//
//    /**
//     * 내 주문 목록 조회 통합 테스트
//     * - GET /api/orders  (X-USER-ID 헤더 기준)
//     */
//    @Test
//    @DisplayName("내 주문 목록 조회 API - X-USER-ID 기준으로 페이징 조회할 수 있다")
//    void getMyOrders_success() throws Exception {
//        // given
//        UUID customerId = UUID.randomUUID();
//
//        // 같은 customerId로 여러 개 저장
//        saveOrderForCustomer(customerId);
//        saveOrderForCustomer(customerId);
//
//        // when & then
//        mockMvc.perform(
//                        get("/api/orders")
//                                .header("X-USER-ID", customerId.toString())
//                                .param("page", "0")
//                                .param("size", "10")
//                )
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
//                .andExpect(jsonPath("$.data.content").isArray())
//                .andExpect(jsonPath("$.data.content.length()").value(2));
//    }
//
//    // ================== 헬퍼 메서드 ==================
//
//    private Order createAndSaveOrder() {
//        UUID customerId = UUID.randomUUID();
//
//        Order order = Order.create(
//                customerId,
//                UUID.randomUUID(), // supplierCompanyId
//                UUID.randomUUID(), // supplierHubId
//                UUID.randomUUID(), // receiptCompanyId
//                UUID.randomUUID(), // receiptHubId
//                UUID.randomUUID(), // productId
//                10_000L,
//                1,
//                LocalDateTime.now().plusDays(1),
//                "서울시 어딘가 1-1",
//                "요청 메모",
//                "홍길동",
//                "010-0000-0000",
//                "slack@example.com"
//        );
//        return orderRepository.save(order);
//    }
//
//    private void saveOrderForCustomer(UUID customerId) {
//        Order order = Order.create(
//                customerId,
//                UUID.randomUUID(),
//                UUID.randomUUID(),
//                UUID.randomUUID(),
//                UUID.randomUUID(),
//                UUID.randomUUID(),
//                5_000L,
//                2,
//                LocalDateTime.now().plusDays(1),
//                "서울시 어딘가 2-2",
//                "요청 메모",
//                "홍길동",
//                "010-0000-0000",
//                "slack@example.com"
//        );
//        orderRepository.save(order);
//    }
//}

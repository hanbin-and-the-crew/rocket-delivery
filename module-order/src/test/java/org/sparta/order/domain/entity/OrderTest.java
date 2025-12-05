//package org.sparta.order.domain.entity;
//
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.sparta.common.error.BusinessException;
//import org.sparta.order.domain.enumeration.CanceledReasonCode;
//import org.sparta.order.domain.enumeration.OrderStatus;
//
//import java.time.LocalDateTime;
//import java.util.UUID;
//
//import static org.assertj.core.api.Assertions.*;
//
//class OrderTest {
//
//    private static final UUID CUSTOMER_ID = UUID.randomUUID();
//    private static final UUID SUPPLIER_COMPANY_ID = UUID.randomUUID();
//    private static final UUID SUPPLIER_HUB_ID = UUID.randomUUID();
//    private static final UUID RECEIPT_COMPANY_ID = UUID.randomUUID();
//    private static final UUID RECEIPT_HUB_ID = UUID.randomUUID();
//    private static final UUID PRODUCT_ID = UUID.randomUUID();
//
//    @Test
//    @DisplayName("주문 생성 시 CREATED 상태로 생성되고 VO들이 올바르게 매핑된다")
//    void createOrder_success() {
//        // given
//        long productPrice = 10_000L;
//        int quantity = 3;
//        LocalDateTime dueAt = LocalDateTime.now().plusDays(1);
//
//        // when
//        Order order = Order.create(
//                CUSTOMER_ID,
//                SUPPLIER_COMPANY_ID,
//                SUPPLIER_HUB_ID,
//                RECEIPT_COMPANY_ID,
//                RECEIPT_HUB_ID,
//                PRODUCT_ID,
//                productPrice,
//                quantity,
//                dueAt,
//                "서울시 어딘가 123-45",
//                "부탁사항",
//                "홍길동",
//                "010-0000-0000",
//                "slack@example.com"
//        );
//
//        // then
//        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CREATED);
//        assertThat(order.getQuantity().getValue()).isEqualTo(quantity);
//        assertThat(order.getProductPriceSnapshot().getAmount()).isEqualTo(productPrice);
//        assertThat(order.getTotalPrice().getAmount()).isEqualTo(productPrice * quantity);
//        assertThat(order.getDueAt().getTime()).isEqualTo(dueAt);
//    }
//
//    @Test
//    @DisplayName("CREATED 상태에서만 APPROVED로 변경 가능하다")
//    void approve_createdOnly() {
//        // given
//        Order order = createDefaultOrder();
//
//        // when
//        order.approve();
//
//        // then
//        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.APPROVED);
//    }
//
//    @Test
//    @DisplayName("APPROVED 상태에서만 SHIPPED로 변경 가능하다")
//    void markShipped_approvedOnly() {
//        // given
//        Order order = createDefaultOrder();
//        order.approve();
//
//        // when
//        order.markShipped();
//
//        // then
//        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.SHIPPED);
//    }
//
//    @Test
//    @DisplayName("SHIPPED 상태에서만 DELIVERED로 변경 가능하다")
//    void markDelivered_shippedOnly() {
//        // given
//        Order order = createDefaultOrder();
//        order.approve();
//        order.markShipped();
//
//        // when
//        order.markDelivered();
//
//        // then
//        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.DELIVERED);
//    }
//
//    @Test
//    @DisplayName("CREATED 또는 APPROVED 상태에서만 취소 가능하고, 취소 사유 코드/메모는 필수이다")
//    void cancel_success() {
//        // given
//        Order order = createDefaultOrder();
//
//        // when
//        order.cancel(CanceledReasonCode.CUSTOMER_REQUEST, "고객 요청 취소");
//
//        // then
//        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELED);
//        assertThat(order.getCanceledReasonCode()).isEqualTo(CanceledReasonCode.CUSTOMER_REQUEST);
//        assertThat(order.getCanceledReasonMemo()).isEqualTo("고객 요청 취소");
//        assertThat(order.getCanceledAt()).isNotNull();
//    }
//
//    @Test
//    @DisplayName("SHIPPED 또는 DELIVERED 상태인 주문은 삭제할 수 없다")
//    void validateDeletable_shippedOrDeliveredFail() {
//        // SHIPPED
//        Order shipped = createDefaultOrder();
//        shipped.approve();
//        shipped.markShipped();
//
//        // DELIVERED
//        Order delivered = createDefaultOrder();
//        delivered.approve();
//        delivered.markShipped();
//        delivered.markDelivered();
//
//        assertThatThrownBy(shipped::validateDeletable)
//                .isInstanceOf(BusinessException.class);
//
//        assertThatThrownBy(delivered::validateDeletable)
//                .isInstanceOf(BusinessException.class);
//    }
//
//    private Order createDefaultOrder() {
//        return Order.create(
//                CUSTOMER_ID,
//                SUPPLIER_COMPANY_ID,
//                SUPPLIER_HUB_ID,
//                RECEIPT_COMPANY_ID,
//                RECEIPT_HUB_ID,
//                PRODUCT_ID,
//                10_000L,
//                1,
//                LocalDateTime.now().plusDays(1),
//                "서울시 어딘가 123-45",
//                "요청",
//                "홍길동",
//                "010-0000-0000",
//                "slack@example.com"
//        );
//    }
//}

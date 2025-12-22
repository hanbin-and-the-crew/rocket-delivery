//package org.sparta.order.infrastructure.repository;
//
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.sparta.order.domain.entity.Order;
//import org.sparta.order.domain.repository.OrderRepository;
//import org.sparta.order.OrderApplication;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//import org.springframework.context.annotation.Import;
//
//import java.time.LocalDateTime;
//import java.util.UUID;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@DataJpaTest
//@Import({OrderRepositoryImpl.class}) // OrderJpaRepository는 @DataJpaTest가 자동으로 스캔
//class OrderRepositoryImplTest {
//
//    @Autowired
//    private OrderRepository orderRepository;
//
//    @Test
//    @DisplayName("고객 ID와 deletedAt이 null인 주문 목록을 페이징 조회할 수 있다")
//    void findByCustomerIdAndDeletedAtIsNull() {
//        // given
//        UUID customerId = UUID.randomUUID();
//
//        Order order = Order.create(
//                customerId,
//                UUID.randomUUID(),
//                UUID.randomUUID(),
//                UUID.randomUUID(),
//                UUID.randomUUID(),
//                UUID.randomUUID(),
//                10_000L,
//                1,
//                LocalDateTime.now().plusDays(1),
//                "서울시 어딘가 1-1",
//                "요청",
//                "홍길동",
//                "010-0000-0000",
//                "slack@example.com"
//        );
//        orderRepository.save(order);
//
//        // when
//        var page = orderRepository.findByCustomerIdAndDeletedAtIsNull(
//                customerId,
//                org.springframework.data.domain.PageRequest.of(0, 10)
//        );
//
//        // then
//        assertThat(page.getContent()).hasSize(1);
//        assertThat(page.getContent().get(0).getCustomerId()).isEqualTo(customerId);
//    }
//}

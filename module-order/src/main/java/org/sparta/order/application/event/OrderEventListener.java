package org.sparta.order.application.event;

import lombok.extern.slf4j.Slf4j;
import org.sparta.order.infrastructure.event.publisher.OrderCreatedSpringEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderEventListener {
    // Spring Event Listener
    // @EventListener: 이 메서드가 OrderCreatedEvent를 구독함을 선언
    @EventListener
    public void handleOrderCreated(OrderCreatedSpringEvent event) {
        log.info("주문 생성 이벤트 수신 - 주문 ID: {}, 상품 ID: {}, 수량: {}",
                event.orderId(),
                event.productId(),
                event.quantity()
        );
    }
}
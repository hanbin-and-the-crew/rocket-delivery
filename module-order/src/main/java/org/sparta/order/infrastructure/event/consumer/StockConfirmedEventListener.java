package org.sparta.order.infrastructure.event.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.order.application.service.OrderService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockConfirmedEventListener {

    private final OrderService orderService;

    // TODO: 실제 토픽명 확인 필요
    @KafkaListener(topics = "stock-events", groupId = "order-service")
    public void handleStockConfirmed(StockConfirmedEvent event) {
        log.info("재고 감소 확정 이벤트 수신 - orderId={}", event.orderId());

        // 재고 감소 완료 → 주문 확정 상태로 전환
        orderService.approveOrder(event.orderId());
    }
}

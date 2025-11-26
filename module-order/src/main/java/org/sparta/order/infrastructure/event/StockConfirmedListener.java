//package org.sparta.order.infrastructure.event;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.sparta.order.application.service.OrderService;
//import org.sparta.order.infrastructure.event.consumer.StockConfirmedEvent;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.stereotype.Component;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class StockConfirmedListener {
//
//    private final OrderService orderService;
//
//    @KafkaListener(topics = "stock-confirmed", groupId = "order-service")
//    public void handleStockConfirmed(StockConfirmedEvent event) {
//        log.info("재고 확정 이벤트 수신 - orderId={}", event.orderId());
//        orderService.approveOrder(event.orderId());
//    }
//}

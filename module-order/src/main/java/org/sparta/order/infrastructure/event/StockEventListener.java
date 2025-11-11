package org.sparta.order.infrastructure.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.order.infrastructure.event.dto.*;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.sparta.order.application.service.OrderService;

/**
 * Product => Order 이벤트 발행
 * 재고 예약, 실패, 취소 등의 상태를 알려줌
 * 메시지 수신 역할 (listener)
 *
 * 주문 관련 이벤트 발행
 * - 주문 생성 : 재고 예약 요청 -> 결과 수신
 * - 주문 취소 : 재고 예약취소 요청 => 결과 수신
 * - 주문 변경 : 재고 예약변경 요청 => 결과 수신
 * - 주문 확정(출고) : 주문건 출고 완료 알림 => 결과 수신
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class StockEventListener {

    private final OrderService orderService;

    @KafkaListener(topics = "stock-reserved", groupId = "order-service")
    public void handleStockReserved(StockEventDto.StockReserved event) {
        orderService.markOrderPlaced(event.orderId());
    }

    @KafkaListener(topics = "stock-reservation-failed", groupId = "order-service")
    public void handleStockReservationFailed(StockEventDto.StockReservationFailed event) {
        orderService.handleStockReservationFailed(event.orderId(), event.message());
    }

    @KafkaListener(topics = "order.quantity.changed", groupId = "order-group")
    public void handleOrderQuantityChanged(OrderQuantityChangedEvent event) {
        log.info("수량 변경 이벤트 수신 - orderId: {}, productId: {}, quantity: {}", event.orderId(), event.productId(), event.quantity());

        // 재고 예약 변경 처리 
        // product 쪽에서는 따로 예약 변경이 없음 => 이거는 재고 예약으로 통일해서 사용해야 될듯? 아마도,,
        // 추후 product 쪽에서 문제가 되는거면 수량 변경 기능은 없애버려도 될듯
    }

    @KafkaListener(topics = "order.canceled", groupId = "order-group")
    public void handleOrderCanceled(OrderCanceledEvent event) {
        log.info("주문 취소 이벤트 수신 - orderId: {}, productId: {}, quantity: {}", event.orderId(), event.productId(), event.quantity());

        // 재고 예약 취소 처리
    }

    @KafkaListener(topics = "order.dispatched", groupId = "order-group")
    public void handleOrderDispatched(OrderDispatchedEvent event) {
        log.info("출고 이벤트 수신 - orderId: {}, productId: {}, quantity: {}", event.orderId(), event.productId(), event.quantity());

        // 실제 재고 감소 이벤트 발행 (Product Service)
    }
}

package org.sparta.order.infrastructure.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.order.application.service.OrderService;
import org.sparta.order.infrastructure.event.dto.StockEventDto;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Product 모듈로부터 재고 이벤트를 수신
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockEventListener {

    private final OrderService orderService;

    /**
     * 재고 예약 성공 이벤트 처리
     */
    @KafkaListener(topics = "stock-reserved", groupId = "order-service")
    public void handleStockReserved(StockEventDto.StockReserved event) {
        log.info("재고 예약 성공 이벤트 수신 - orderId: {}, productId: {}, reserved: {}",
                event.orderId(), event.productId(), event.reservedQuantity());

        // 주문 상태를 업데이트하거나 추가 처리
        // 현재는 로깅만 수행
    }

    /**
     * 재고 예약 실패 이벤트 처리
     */
    @KafkaListener(topics = "stock-reservation-failed", groupId = "order-service")
    public void handleStockReservationFailed(StockEventDto.StockReservationFailed event) {
        log.error("재고 예약 실패 이벤트 수신 - orderId: {}, productId: {}, reason: {}",
                event.orderId(), event.productId(), event.message());

        // 주문을 자동으로 취소하거나 실패 처리
        try {
            orderService.handleStockReservationFailed(
                    event.orderId(),
                    event.message()
            );
        } catch (Exception e) {
            log.error("재고 예약 실패 처리 중 오류 발생 - orderId: {}", event.orderId(), e);
        }
    }

    /**
     * 재고 확정 완료 이벤트 처리
     */
    @KafkaListener(topics = "stock-confirmed", groupId = "order-service")
    public void handleStockConfirmed(StockEventDto.StockConfirmed event) {
        log.info("재고 확정 완료 이벤트 수신 - orderId: {}, productId: {}, confirmed: {}",
                event.orderId(), event.productId(), event.confirmedQuantity());

        // 주문 상태를 DISPATCHED로 변경하거나 배송 시작
        // 현재는 로깅만 수행
    }

    /**
     * 재고 예약 취소 완료 이벤트 처리
     */
    @KafkaListener(topics = "stock-reservation-cancelled", groupId = "order-service")
    public void handleStockReservationCancelled(StockEventDto.StockReservationCancelled event) {
        log.info("재고 예약 취소 완료 이벤트 수신 - orderId: {}, productId: {}, cancelled: {}",
                event.orderId(), event.productId(), event.cancelledQuantity());

        // 주문 취소 완료 처리
        // 현재는 로깅만 수행
    }
}
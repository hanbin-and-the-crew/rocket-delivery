package org.sparta.order.application.event;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.order.domain.entity.Order;
import org.sparta.order.domain.entity.ProcessedEvent;
import org.sparta.order.domain.repository.ProcessedEventRepository;
import org.sparta.order.infrastructure.event.StockConfirmedEvent;
import org.sparta.order.infrastructure.event.StockReservationCancelledEvent;
import org.sparta.order.infrastructure.event.StockReservationFailedEvent;
import org.sparta.order.infrastructure.event.StockReservedEvent;
import org.sparta.order.infrastructure.event.publisher.*;
import org.sparta.order.infrastructure.repository.OrderJpaRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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
@RequiredArgsConstructor
@Component("orderStockEventListener")
public class StockEventListener {

    private final OrderJpaRepository orderRepository;
    private final ProcessedEventRepository processedEventRepository;

    @KafkaListener(topics = "stock-reserved", groupId = "order-service")
    @Transactional
    public void handleStockReserved(StockReservedEvent event) {
        // 1. 멱등성 체크 (eventId로 중복 처리 방지)
        if (processedEventRepository.existsByEventId(event.eventId())) {
            log.warn("이미 처리된 이벤트 - eventId: {}", event.eventId());
            return;
        }

        // 2. 주문 상태 변경
        Order order = orderRepository.findById(event.orderId())
                .orElseThrow(() -> new EntityNotFoundException("주문을 찾을 수 없습니다. id=" + event.orderId()));
        order.markAsStockReserved();  // PENDING → STOCK_RESERVED

        // 3. 결제 대기 상태로 전환 (타임아웃 설정 권장)
        // 예: 10분 내 미결제 시 자동 취소

        // 4. 처리 완료 기록
        processedEventRepository.save(
                ProcessedEvent.of(event.eventId(), "StockReservedEvent")
        );

        log.info("재고 예약 성공 처리 완료 - orderId: {}", event.orderId());
    }

    @KafkaListener(topics = "stock-reservation-failed", groupId = "order-service")
    @Transactional
    public void handleStockReservationFailed(StockReservationFailedEvent event) {
        // 1. 멱등성 체크
        if (processedEventRepository.existsByEventId(event.eventId())) {
            log.warn("이미 처리된 이벤트 - eventId: {}", event.eventId());
            return;
        }

        // 2. 주문 취소 처리
        UUID userId = UUID.randomUUID(); // UserId를 Order에서 어떻게 관리해야할 것인가

        Order order = orderRepository.findById(event.orderId())
                .orElseThrow(() -> new EntityNotFoundException("주문을 찾을 수 없습니다. id=" + event.orderId()));
        order.cancel(order.getId(), userId, order.getCanceledReasonCode() ,event.reason());  // PENDING → CANCELLED

        // 3. 사용자에게 실패 알림 (재고 부족 안내)
//        notificationService.sendOrderFailedNotification(
//                userId,
//                event.reason()
//        );

        // 4. 처리 완료 기록
        processedEventRepository.save(
                ProcessedEvent.of(event.eventId(), "StockReservationFailedEvent")
        );

        log.info("재고 예약 실패 처리 완료 - orderId: {}, reason: {}",
                event.orderId(), event.reason());
    }

    @KafkaListener(topics = "stock-confirmed", groupId = "order-service")
    @Transactional
    public void handleStockConfirmed(StockConfirmedEvent event) {
        // 1. 멱등성 체크
        if (processedEventRepository.existsByEventId(event.eventId())) {
            log.warn("이미 처리된 이벤트 - eventId: {}", event.eventId());
            return;
        }

        // 2. 주문 상태 변경
        Order order = orderRepository.findById(event.orderId())
                .orElseThrow(() -> new EntityNotFoundException("주문을 찾을 수 없습니다. id=" + event.orderId()));
        order.confirm();  // STOCK_RESERVED → CONFIRMED

        // 3. 배송 준비 이벤트 발행 (Optional)
//        kafkaTemplate.send("delivery-requested",
//                new DeliveryRequestedEvent(order.getId(), order.getDeliveryAddress())
//        );

        // 4. 처리 완료 기록
        processedEventRepository.save(
                ProcessedEvent.of(event.eventId(), "StockConfirmedEvent")
        );

        log.info("재고 확정 처리 완료 - orderId: {}", event.orderId());
    }

    @KafkaListener(topics = "stock-reservation-cancelled", groupId = "order-service")
    @Transactional
    public void handleStockReservationCancelled(StockReservationCancelledEvent event) {
        // 1. 멱등성 체크
        if (processedEventRepository.existsByEventId(event.eventId())) {
            log.warn("이미 처리된 이벤트 - eventId: {}", event.eventId());
            return;
        }

        // 2. 주문 취소 최종 확정
        Order order = orderRepository.findById(event.orderId())
                .orElseThrow(() -> new EntityNotFoundException("주문을 찾을 수 없습니다. id=" + event.orderId()));
        order.cancel();  // CANCELLING → CANCELLED

        // 3. 환불 처리 (필요 시)
//        if (order.isPaid()) {
//            refundService.processRefund(order.getId());
//        }

        // 4. 처리 완료 기록
        processedEventRepository.save(
                ProcessedEvent.of(event.eventId(), "StockReservationCancelledEvent")
        );

        log.info("재고 예약 취소 처리 완료 - orderId: {}", event.orderId());
    }
}

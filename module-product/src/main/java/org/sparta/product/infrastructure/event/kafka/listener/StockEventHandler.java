package org.sparta.product.infrastructure.event.kafka.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.error.BusinessException;
import org.sparta.product.application.service.StockService;
import org.sparta.product.domain.event.ProcessedEvent;
import org.sparta.product.domain.event.ProductOutboxEvent;
import org.sparta.product.domain.entity.Stock;
import org.sparta.product.domain.repository.ProcessedEventRepository;
import org.sparta.product.infrastructure.event.kafka.dto.*;
import org.sparta.product.domain.enums.OutboxStatus;
import org.sparta.product.infrastructure.event.outbox.ProductOutboxEventRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Product 모듈 재고 이벤트 핸들러
 *
 * Product 관점에서:
 * - 주문 생성, 결제 완료, 주문 취소 이벤트를 수신하여 재고를 처리
 * - 처리 결과에 따라 재고 이벤트를 발행
 *
 * Product 모듈 역할:
 * 1. 재고 예약 (OrderCreatedEvent 수신 시)
 * 2. 재고 확정 (PaymentCompletedEvent 수신 시)
 * 3. 예약 취소 (OrderCancelledEvent 수신 시)
 * 4. 재고 처리 결과 이벤트 발행
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockEventHandler {

    private final StockService stockService;
    private final ProcessedEventRepository processedEventRepository;

//    private final EventPublisher eventPublisher;
    private final ProductOutboxEventRepository productOutboxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     *  재고 예약 처리
     * - 수신: OrderCreatedEvent (Order 모듈 발행)
     * - 처리: 재고 예약
     * - 발행: StockReservedEvent 또는 StockReservationFailedEvent
     */
    @KafkaListener(topics = "order-created", groupId = "product-service")
    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("주문 생성 이벤트 수신 - orderId: {}, productId: {}, quantity: {}",
                event.orderId(), event.productId(), event.quantity());

        if (processedEventRepository.existsByEventId(event.eventId())) {
            log.warn("이미 처리된 이벤트 - eventId: {}", event.eventId());
            return;
        }

        try {
            Stock stock = stockService.getStock(event.productId());
            stockService.reserveStock(event.productId(), event.quantity());

            // 멱등성 기록
            processedEventRepository.save(
                    ProcessedEvent.of(event.eventId(), "OrderCreatedEvent")
            );

            // 재고 예약 성공 이벤트 발행
            StockReservedEvent successEvent = StockReservedEvent.of(
                    event.orderId(),
                    event.productId(),
                    event.quantity(),
                    stock.getAvailableQuantity(),
                    stock.getStatus()
            );
//            eventPublisher.publishExternal(successEvent);
            saveOutboxEvent(
                    successEvent,
                    successEvent.eventId(),
                    successEvent.productId(),
                    successEvent.occurredAt()
            );

            log.info("재고 예약 성공 - orderId: {}, productId: {}, reserved: {}, available: {}",
                    event.orderId(), event.productId(), event.quantity(), stock.getAvailableQuantity());

        } catch (BusinessException e) {
            // 재고 예약 실패 이벤트 발행
            StockReservationFailedEvent failEvent = StockReservationFailedEvent.of(
                    event.orderId(),
                    event.productId(),
                    event.quantity(),
                    e.getMessage()
            );
//            eventPublisher.publishExternal(failEvent);
            saveOutboxEvent(
                    failEvent,
                    failEvent.eventId(),
                    failEvent.productId(),
                    failEvent.occurredAt()
            );

            // 멱등성 기록
            processedEventRepository.save(
                    ProcessedEvent.of(event.eventId(), "OrderCreatedEvent")
            );

            log.error("재고 예약 실패 - orderId: {}, productId: {}, reason: {}",
                    event.orderId(), event.productId(), e.getMessage());
        }
    }

    /**
     *  재고 확정 처리
     * - 수신: PaymentCompletedEvent (Payment/Order 모듈 발행)
     * - 처리: 재고 실제 차감
     * - 발행: StockConfirmedEvent
     */
    @KafkaListener(topics = "payment-completed", groupId = "product-service")
    @Transactional
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("결제 완료 이벤트 수신 - orderId: {}, productId: {}, quantity: {}",
                event.orderId(), event.productId(), event.quantity());

        if (processedEventRepository.existsByEventId(event.eventId())) {
            log.warn("이미 처리된 이벤트 - eventId: {}", event.eventId());
            return;
        }

        try {
            Stock stock = stockService.getStock(event.productId());
            stockService.confirmReservation(event.productId(), event.quantity());

            // 멱등성 기록
            processedEventRepository.save(
                    ProcessedEvent.of(event.eventId(), "PaymentCompletedEvent")
            );

            // 재고 확정 이벤트 발행
            StockConfirmedEvent confirmedEvent = StockConfirmedEvent.of(
                    event.orderId(),
                    event.productId(),
                    event.quantity(),
                    stock.getQuantity()
            );
//            eventPublisher.publishExternal(confirmedEvent);
            saveOutboxEvent(
                    confirmedEvent,
                    confirmedEvent.eventId(),
                    confirmedEvent.productId(),
                    confirmedEvent.occurredAt()
            );

            log.info("재고 확정 완료 - orderId: {}, productId: {}, confirmed: {}, remaining: {}",
                    event.orderId(), event.productId(), event.quantity(), stock.getQuantity());

        } catch (BusinessException e) {
            // 멱등성 기록
            processedEventRepository.save(
                    ProcessedEvent.of(event.eventId(), "PaymentCompletedEvent")
            );

            log.error("재고 확정 실패 - orderId: {}, productId: {}, reason: {}",
                    event.orderId(), event.productId(), e.getMessage());
            throw e; // 재시도 가능하도록 예외 던지기
        }
    }

    /**
     * 예약 취소 처리
     * - 수신: OrderCancelledEvent (Order 모듈 발행)
     * - 처리: 재고 예약 취소
     * - 발행: StockReservationCancelledEvent
     */
    @KafkaListener(topics = "order-cancelled", groupId = "product-service")
    @Transactional
    public void handleOrderCancelled(OrderCancelledEvent event) {
        log.info("주문 취소 이벤트 수신 - orderId: {}, productId: {}, quantity: {}",
                event.orderId(), event.productId(), event.quantity());

        if (processedEventRepository.existsByEventId(event.eventId())) {
            log.warn("이미 처리된 이벤트 - eventId: {}", event.eventId());
            return;
        }

        try {
            stockService.cancelReservation(event.productId(), event.quantity());

            // 멱등성 기록
            processedEventRepository.save(
                    ProcessedEvent.of(event.eventId(), "OrderCancelledEvent")
            );

            // 예약 취소 이벤트 발행
            StockReservationCancelledEvent cancelledEvent = StockReservationCancelledEvent.of(
                    event.orderId(),
                    event.productId(),
                    event.quantity()
            );
//            eventPublisher.publishExternal(cancelledEvent);
            saveOutboxEvent(
                    cancelledEvent,
                    cancelledEvent.eventId(),
                    cancelledEvent.productId(),
                    cancelledEvent.occurredAt()
            );


            log.info("재고 예약 취소 완료 - orderId: {}, productId: {}, cancelled: {}",
                    event.orderId(), event.productId(), event.quantity());

        } catch (BusinessException e) {
            // 멱등성 기록
            processedEventRepository.save(
                    ProcessedEvent.of(event.eventId(), "OrderCancelledEvent")
            );

            log.error("재고 예약 취소 실패 - orderId: {}, productId: {}, reason: {}",
                    event.orderId(), event.productId(), e.getMessage());
        }
    }



    private void saveOutboxEvent(Object event, UUID eventId, UUID aggregateId, Instant occurredAt) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            ProductOutboxEvent outbox = ProductOutboxEvent.builder()
                    .eventId(eventId)
                    .eventType(event.getClass().getSimpleName())
                    .aggregateId(aggregateId)
                    .payload(payload)
                    .status(OutboxStatus.READY)
                    .occurredAt(occurredAt)
                    .build();

            productOutboxEventRepository.save(outbox);

        } catch (Exception e) {
            log.error("[Outbox] 이벤트 직렬화/저장 실패 - eventId={}", eventId, e);
            // 여기서 비즈니스적으로 어떻게 할지 결정:
            //  - 런타임 예외 던져서 전체 트랜잭션 롤백
            //  - 아니면 로그만 남기고 진행 (지금 과제 기준이면 롤백이 더 안전)
            throw new RuntimeException("Outbox 저장 실패", e);
        }
    }







}

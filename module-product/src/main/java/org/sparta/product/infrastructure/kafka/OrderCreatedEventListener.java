package org.sparta.product.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.sparta.common.error.BusinessException;
import org.sparta.common.event.EventPublisher;
import org.sparta.product.application.service.StockService;
import org.sparta.product.domain.entity.Stock;
import org.sparta.product.domain.entity.StockReservation;
import org.sparta.product.domain.event.StockConfirmedEvent;
import org.sparta.product.domain.repository.StockRepository;
import org.sparta.product.domain.repository.StockReservationRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.sparta.product.domain.outbox.ProductOutboxEvent;
import org.sparta.product.domain.repository.ProductOutboxEventRepository;
import org.springframework.transaction.annotation.Transactional;
import org.sparta.product.domain.event.StockReservationFailedEvent;


import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Order 모듈에서 발행하는 OrderCreatedEvent(order-created 토픽)를 소비해서
 * - 해당 주문의 재고 예약을 확정(실차감)하고
 * - 재고 확정 완료 이벤트(StockConfirmedEvent)를 발행하는 리스너.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedEventListener {

    private final StockService stockService;
    private final StockReservationRepository stockReservationRepository;
    private final StockRepository stockRepository;
    private final EventPublisher eventPublisher;

    private final ProductOutboxEventRepository productOutboxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * order-created 토픽 리스너
     */
    @KafkaListener(topics = "order-created", groupId = "product-service")
    @Transactional
    public void handleOrderCreated(ConsumerRecord<String, Object> record) {

        Object rawValue = record.value();

        if (!(rawValue instanceof Map<?, ?> rawMap)) {
            log.warn("[OrderCreatedEventListener] 예기치 않은 payload 타입 수신: type={}, value={}",
                    (rawValue != null ? rawValue.getClass() : "null"),
                    rawValue);
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> event = (Map<String, Object>) rawMap;

        // 1. orderId 추출
        Object orderIdRaw = event.get("orderId");
        if (orderIdRaw == null) {
            log.warn("[OrderCreatedEventListener] orderId 없는 이벤트 수신: {}", event);
            return;
        }

        String orderIdStr = orderIdRaw.toString();

        UUID orderId;
        try {
            orderId = UUID.fromString(orderIdStr);
        } catch (IllegalArgumentException ex) {
            log.warn("[OrderCreatedEventListener] 잘못된 orderId 형식: {}", orderIdStr, ex);
            return;
        }

        // 현재 규약: reservationKey == orderId.toString()
        String reservationKey = orderIdStr;

        log.info("[OrderCreatedEventListener] 주문완료 이벤트 수신 - orderId={}, reservationKey={}",
                orderId, reservationKey);


        // 2. 재고 예약 확정(실차감)
        try {
            // 내부에서 낙관적 락 + @Retryable + @Transactional 처리
            stockService.confirmReservation(reservationKey);

        } catch (BusinessException ex) {
            // 도메인 예외는 경고 로그 + 실패 이벤트 Outbox 저장
            log.warn("[OrderCreatedEventListener] 재고 확정 실패 - reservationKey={}, errorType={}",
                    reservationKey, ex.getErrorType(), ex);

            publishStockReservationFailedEvent(
                    orderId,
                    reservationKey,
                    ex.getErrorType().getCode(),
                    ex.getMessage()
            );
            return;

        } catch (Exception ex) {
            // 알 수 없는 예외는 Kafka 컨테이너 죽지 않도록 로그만 남기고 종료
            log.error("[OrderCreatedEventListener] 재고 확정 중 알 수 없는 오류 - reservationKey={}",
                    reservationKey, ex);
            return;
        }



        log.info("[OrderCreatedEventListener] 재고 확정 성공 - reservationKey={}", reservationKey);

        // 3. 재고 확정 완료 이벤트 발행을 위한 추가 조회
        //    (confirmReservation 내부에서도 동일 조회를 하지만,
        //     이벤트 payload 구성을 위해 다시 한번 읽어온다.)

        Optional<StockReservation> reservationOpt =
                stockReservationRepository.findByReservationKey(reservationKey);

        if (reservationOpt.isEmpty()) {
            log.warn("[OrderCreatedEventListener] 재고 예약을 찾을 수 없음 - reservationKey={}", reservationKey);
            return;
        }

        StockReservation reservation = reservationOpt.get();

        Optional<Stock> stockOpt = stockRepository.findById(reservation.getStockId());
        if (stockOpt.isEmpty()) {
            log.warn("[OrderCreatedEventListener] Stock 엔티티를 찾을 수 없음 - stockId={}",
                    reservation.getStockId());
            return;
        }

        Stock stock = stockOpt.get();

        // 4. 재고 확정 완료 이벤트 구성
        StockConfirmedEvent stockConfirmedEvent = StockConfirmedEvent.of(
                orderId,
                stock.getProductId(),
                reservation.getReservedQuantity()
        );

        // 5. Outbox 에 이벤트 저장 (재고 차감과 같은 트랜잭션 내)
        try {
            String payloadJson = objectMapper.writeValueAsString(stockConfirmedEvent);

            ProductOutboxEvent outboxEvent =
                    ProductOutboxEvent.stockConfirmed(stockConfirmedEvent, payloadJson);

            productOutboxEventRepository.save(outboxEvent);

            log.info("[OrderCreatedEventListener] StockConfirmedEvent Outbox 저장 완료 - outboxId={}, orderId={}, productId={}, quantity={}",
                    outboxEvent.getId(), orderId, stock.getProductId(), reservation.getReservedQuantity());

        } catch (JsonProcessingException ex) {
            // 직렬화 실패하면 Kafka 발행도 불가능하므로, 일단 트랜잭션 롤백을 막기 위해 여기서만 로그 후 종료
            log.error("[OrderCreatedEventListener] StockConfirmedEvent 직렬화 실패 - orderId={}, productId={}",
                    orderId, stock.getProductId(), ex);
        }
    }




    /**
     * 재고 확정/차감 실패 시 실패 이벤트를 Outbox 에 저장한다.
     *
     * - 이 메서드는 Kafka 리스너의 트랜잭션 컨텍스트 안에서 실행된다.
     * - 이후 ProductOutboxPublisher 가 READY 상태 이벤트를 읽어 외부(Kafka 등)로 발행한다.
     */
    private void publishStockReservationFailedEvent(
            UUID orderId,
            String reservationKey,
            String errorCode,
            String errorMessage
    ) {
        StockReservationFailedEvent event =
                StockReservationFailedEvent.of(orderId, reservationKey, errorCode, errorMessage);

        try {
            String payloadJson = objectMapper.writeValueAsString(event);

            ProductOutboxEvent outboxEvent =
                    ProductOutboxEvent.stockReservationFailed(event, payloadJson);

            productOutboxEventRepository.save(outboxEvent);

            log.info("[OrderCreatedEventListener] StockReservationFailedEvent Outbox 저장 완료 - outboxId={}, orderId={}, reservationKey={}, errorCode={}",
                    outboxEvent.getId(), orderId, reservationKey, errorCode);

        } catch (JsonProcessingException ex) {
            // 직렬화 실패하면 Kafka 발행도 불가능하므로, 일단 트랜잭션 롤백을 막기 위해 여기서만 로그 후 종료
            log.error("[OrderCreatedEventListener] StockReservationFailedEvent 직렬화 실패 - orderId={}, reservationKey={}",
                    orderId, reservationKey, ex);
        }
    }


}

package org.sparta.product.infrastructure.kafka;

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

    /**
     * order-created 토픽 리스너
     *
     * KafkaConfig 설정 기준:
     * - value는 JsonDeserializer + LinkedHashMap 으로 역직렬화됨
     *   → record.value()는 Map<String, Object> 형태라고 보면 된다.
     */
    @KafkaListener(topics = "order-created", groupId = "product-service")
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
            // 내부에서 낙관적 락 + @Retryable + @Transactional 처리됨
            stockService.confirmReservation(reservationKey);
        } catch (BusinessException ex) {
            // 도메인 예외는 경고 로그만 남기고 종료
            log.warn("[OrderCreatedEventListener] 재고 확정 실패 - reservationKey={}, errorType={}",
                    reservationKey, ex.getErrorType(), ex);
            return;
        } catch (Exception ex) {
            // 알 수 없는 예외도 여기서 잡고 로그만 남김 (Kafka 컨테이너 죽지 않도록)
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

        // 5. 외부 이벤트 발행 (Kafka: stock-events 토픽)
        try {
            eventPublisher.publishExternal(stockConfirmedEvent);
            log.info("[OrderCreatedEventListener] StockConfirmedEvent 발행 완료 - orderId={}, productId={}, quantity={}",
                    orderId, stock.getProductId(), reservation.getReservedQuantity());
        } catch (Exception ex) {
            // 재고는 이미 차감됐고, 이벤트 발행만 실패한 상황
            // (추후 outbox 패턴으로 개선할 수 있는 포인트)
            log.error("[OrderCreatedEventListener] StockConfirmedEvent 발행 실패 - orderId={}, productId={}",
                    orderId, stock.getProductId(), ex);
        }
    }
}

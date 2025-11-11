package org.sparta.order.infrastructure.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 주문 관련 이벤트를 Kafka로 발행.
 * - 주문 생성 → 재고 예약 요청
 * - 주문 취소 → 재고 예약 취소 요청
 *
 * 결과 이벤트(성공/실패/확정/취소완료)는 Product 서비스가 발행하고
 * 본 서비스의 StockEventListener가 구독:
 *   - stock-reserved
 *   - stock-reservation-failed
 *   - stock-confirmed
 *   - stock-reservation-cancelled
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private static final String TOPIC_STOCK_RESERVATION_REQUEST = "stock-reservation-request";
    private static final String TOPIC_STOCK_RESERVATION_CANCEL_REQUEST = "stock-reservation-cancel-request";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /** 주문 생성 → 재고 예약 요청 이벤트 */
    public void publishOrderCreated(UUID orderId, UUID productId, int quantity, UUID userId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "ORDER_CREATED");
        payload.put("orderId", orderId);
        payload.put("productId", productId);
        payload.put("quantity", quantity);
        payload.put("userId", userId);
        payload.put("occurredAt", OffsetDateTime.now().toString());

        log.info("재고 예약 요청 이벤트 발행 - topic: {}, orderId: {}, productId: {}, quantity: {}",
                TOPIC_STOCK_RESERVATION_REQUEST, orderId, productId, quantity);

        kafkaTemplate.send(TOPIC_STOCK_RESERVATION_REQUEST, orderId.toString(), payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("재고 예약 요청 이벤트 발행 실패 - orderId: {}", orderId, ex);
                    } else {
                        log.info("재고 예약 요청 이벤트 발행 성공 - topic: {}, partition: {}, offset: {}, orderId: {}",
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset(),
                                orderId);
                    }
                });
    }

    /** 주문 취소 → 재고 예약 취소 요청 이벤트 */
    public void publishOrderCancelled(UUID orderId, UUID productId, int quantity) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "ORDER_CANCELLED");
        payload.put("orderId", orderId);
        payload.put("productId", productId);
        payload.put("quantity", quantity);
        payload.put("occurredAt", OffsetDateTime.now().toString());

        log.info("재고 예약 취소 요청 이벤트 발행 - topic: {}, orderId: {}, productId: {}, quantity: {}",
                TOPIC_STOCK_RESERVATION_CANCEL_REQUEST, orderId, productId, quantity);

        kafkaTemplate.send(TOPIC_STOCK_RESERVATION_CANCEL_REQUEST, orderId.toString(), payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("재고 예약 취소 요청 이벤트 발행 실패 - orderId: {}", orderId, ex);
                    } else {
                        log.info("재고 예약 취소 요청 이벤트 발행 성공 - topic: {}, partition: {}, offset: {}, orderId: {}",
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset(),
                                orderId);
                    }
                });
    }
}

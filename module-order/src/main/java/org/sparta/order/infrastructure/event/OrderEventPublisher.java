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
 * [ Order => Product로 메시지 발행 ]
 * 메세지 발신 역할 (publisher)
 * 
 * 주문 관련 이벤트를 Kafka로 발행.
 * - 주문 생성 → 재고 예약 요청
 * - 주문 변경 -> 재고 예약 요청 (product쪽에 구현이 안되어있음 / 그냥 생성으로 사용해야될듯)
 * - 주문 취소 → 재고 예약 취소 요청
 * - 주문 확정(출고) -> 주문건 출고 완료 알림
 *
 * 결과 이벤트(성공/실패/확정/취소완료)는 Product 서비스가 발행하고
 * 본 서비스의 StockEventListener가 구독:
 *   - stock-reserved   // 예약 성공
 *   - stock-reservation-failed // 예약 실패 (성공이랑 하나로 묶여있음)
 *   - stock-confirmed  // 예약 확정
 *   - stock-reservation-cancelled  // 예약 취소 완료
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private static final String TOPIC_STOCK_RESERVATION_REQUEST = "stock-reservation-request";
    private static final String TOPIC_STOCK_RESERVATION_CANCEL_REQUEST = "stock-reservation-cancel-request";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // TODO: 재고수량 확인 요청 이벤트


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

    // TODO: 주문 확정(출고) 알림 이벤트 발행 -> product : 실제 재고 수량에서 예약 수량 감소

}

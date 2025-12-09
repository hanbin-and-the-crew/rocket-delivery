// 파일 위치: org/sparta/delivery/infrastructure/event/listener/OrderApprovedListener.java

package org.sparta.delivery.infrastructure.event.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.delivery.application.service.DeliveryService;
import org.sparta.delivery.domain.entity.DeliveryProcessedEvent;
import org.sparta.delivery.domain.repository.DeliveryProcessedEventRepository;
import org.sparta.delivery.infrastructure.event.OrderApprovedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [OrderApprovedEvent 수신]
 * => 주문 승인 시 배송 생성
 * => 허브 경로 계산 및 DeliveryLog 생성
 *
 * 멱등성 보장:
 * - eventId 기반 중복 이벤트 체크
 * - 동일 주문으로 배송 중복 생성 방지
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderApprovedListener {

    private final DeliveryService deliveryService;
    private final DeliveryProcessedEventRepository deliveryProcessedEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "order-events",
            groupId = "delivery-order-approved-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleOrderApproved(String message) {
        try {
            OrderApprovedEvent event = objectMapper.readValue(message, OrderApprovedEvent.class);
            log.info("OrderApprovedEvent received: orderId={}, eventId={}",
                    event.orderId(), event.eventId());

            // 멱등성 체크: eventId로 중복 이벤트 확인
            if (deliveryProcessedEventRepository.existsByEventId(event.eventId())) {
                log.info("Event already processed, skipping: eventId={}, orderId={}",
                        event.eventId(), event.orderId());
                return;
            }

            // 배송 생성 (허브 경로 계산 + DeliveryLog 생성)
            deliveryService.createWithRoute(event);
            log.info("Delivery created successfully: orderId={}, eventId={}",
                    event.orderId(), event.eventId());

            // 이벤트 처리 완료 기록 (같은 트랜잭션)
            deliveryProcessedEventRepository.save(
                    DeliveryProcessedEvent.of(event.eventId(), "ORDER_APPROVED")
            );

            log.info("OrderApprovedEvent processing completed: orderId={}, eventId={}",
                    event.orderId(), event.eventId());

        } catch (Exception e) {
            log.error("Failed to handle order approved event: orderId={}, message={}",
                    extractOrderId(message), message, e);
            // 예외 발생 시 전체 트랜잭션 롤백 + Kafka 재시도
            throw new RuntimeException("Order approved event processing failed", e);
        }
    }

    /**
     * 로그 출력용 orderId 추출 (파싱 실패 시에도 로그 남기기 위함)
     */
    private String extractOrderId(String message) {
        try {
            OrderApprovedEvent event = objectMapper.readValue(message, OrderApprovedEvent.class);
            return event.orderId() != null ? event.orderId().toString() : "unknown";
        } catch (Exception e) {
            return "parse-failed";
        }
    }
}

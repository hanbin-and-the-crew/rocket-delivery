package org.sparta.delivery.application.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.sparta.delivery.domain.entity.DeliveryProcessedEvent;
import org.sparta.delivery.domain.repository.DeliveryProcessedEventRepository;
import org.sparta.delivery.infrastructure.event.OrderApprovedEvent;
import org.sparta.common.event.payment.GenericDomainEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * DLT 토픽 재처리 스케줄러 + Consumer
 * - DLT 메시지 자동 수신
 * - ProcessedEvent 미처리 이벤트만 원본 토픽 재전송
 * - 멱등성 보장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryDltRetryScheduler {

    private final KafkaTemplate<String, String> dltRetryKafkaTemplate;  // DLT 전용 Template
    private final DeliveryProcessedEventRepository deliveryProcessedEventRepository;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    /**
     * 5분마다 DLT 토픽 상태 모니터링 (메트릭스 로그)
     */
    @Scheduled(fixedRate = 300_000) // 5분
    @Transactional(readOnly = true)
    public void monitorDltTopics() {
        log.info("=== DLT 모니터링 실행됨: 시간={}",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }

    // ===========================
    // DLT Consumer: Order.orderApprove.DLT
    // ===========================

    /**
     * OrderApprovedEvent DLT 재처리
     */
    @KafkaListener(
            topics = "order.orderApprove.DLT",
            groupId = "dlt-retry-group",
            containerFactory = "dltRetryKafkaListenerContainerFactory"
    )
    @Transactional
    public void retryOrderApproveDlt(ConsumerRecord<String, String> record, Acknowledgment ack) {
        processDltMessage(record, "order.orderApprove", ack);
    }

    // ===========================
    // DLT Consumer: payment-events.DLT
    // ===========================

    /**
     * PaymentCanceledEvent DLT 재처리
     */
    @KafkaListener(
            topics = "payment-events.DLT",
            groupId = "dlt-retry-group",
            containerFactory = "dltRetryKafkaListenerContainerFactory"
    )
    @Transactional
    public void retryPaymentCancelDlt(ConsumerRecord<String, String> record, Acknowledgment ack) {
        processDltMessage(record, "payment-events", ack);
    }

    /**
     * 공통 DLT 메시지 처리 로직
     */
    private void processDltMessage(ConsumerRecord<String, String> record, String originalTopic, Acknowledgment ack) {
        try {
            String messageValue = record.value();  // 타입 안전!
            UUID eventId = extractEventId(messageValue);

            if (eventId == null) {
                log.warn("DLT 메시지에서 eventId 추출 실패: topic={}, partition={}, offset={}",
                        record.topic(), record.partition(), record.offset());
                ack.acknowledge();
                return;
            }

            // 1. 멱등성 체크 (같은 트랜잭션)
            if (deliveryProcessedEventRepository.existsByEventId(eventId)) {
                log.info("DLT 메시지 이미 처리됨 (멱등성): eventId={}, topic={}", eventId, record.topic());
                ack.acknowledge();
                return;
            }

            // 2. 동기 재전송 + 성공 후 ACK
            dltRetryKafkaTemplate.send(originalTopic, messageValue).get();
            log.info("DLT 재전송 성공: eventId={}, originalTopic={}, offset={}",
                    eventId, originalTopic, record.offset());

            ack.acknowledge();  // 성공 후 ACK (유실 방지!)

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("DLT 재전송 중단: eventId={}, offset={}",
                    extractEventId(record.value()), record.offset(), e);
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            log.error("DLT 재전송 실패: eventId={}, offset={}",
                    extractEventId(record.value()), record.offset(), e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            log.error("DLT 메시지 처리 실패: topic={}, partition={}, offset={}",
                    record.topic(), record.partition(), record.offset(), e);
            // 예외 발생 → ACK 하지 않음 (Kafka 재시도)
        }
    }

    /**
     * JSON 메시지에서 eventId 추출 (GenericDomainEvent 우선 + 순차 시도)
     */
    private UUID extractEventId(String jsonMessage) {
        try {
            // 1. GenericDomainEvent 먼저 (PaymentCanceledEvent)
            try {
                GenericDomainEvent genericEvent = OBJECT_MAPPER.readValue(jsonMessage, GenericDomainEvent.class);
                if (genericEvent.eventId() != null) {
                    return genericEvent.eventId();
                }
            } catch (Exception ignored) {
                // GenericEvent 파싱 실패 → 다음 시도
            }

            // 2. OrderApprovedEvent
            try {
                OrderApprovedEvent orderEvent = OBJECT_MAPPER.readValue(jsonMessage, OrderApprovedEvent.class);
                if (orderEvent.eventId() != null) {
                    return orderEvent.eventId();
                }
            } catch (Exception ignored) {
                // OrderEvent 파싱 실패
            }

            log.warn("모든 eventId 추출 시도 실패");
            return null;
        } catch (Exception e) {
            log.error("eventId 추출 예외: {}", e.getMessage());
            return null;
        }
    }
}

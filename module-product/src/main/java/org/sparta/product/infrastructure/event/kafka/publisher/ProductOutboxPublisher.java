package org.sparta.product.infrastructure.event.kafka.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.product.domain.event.ProductOutboxEvent;
import org.sparta.product.domain.enums.OutboxStatus;
import org.sparta.product.infrastructure.event.outbox.ProductOutboxEventRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductOutboxPublisher {

    private final ProductOutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // 1초마다 실행
    @Scheduled(fixedDelay = 1000L)
    @Transactional
    public void publishOutboxEvents() {

        List<ProductOutboxEvent> events =
                outboxRepository.findTop100ByStatusOrderByOccurredAtAsc(OutboxStatus.READY);

        if (events.isEmpty()) return;

        log.info("[OutboxPublisher] READY 상태 이벤트 {}개 처리 시작", events.size());

        for (ProductOutboxEvent outbox : events) {
            try {
                String topic = resolveTopic(outbox.getEventType());
                String payload = outbox.getPayload();

                kafkaTemplate.send(topic, payload);

                outbox.markPublished();
                log.info("[OutboxPublisher] 이벤트 발행 성공 - id={}, type={}, topic={}",
                        outbox.getEventId(),
                        outbox.getEventType(),
                        topic);

            } catch (Exception ex) {
                outbox.markFailed(ex.getMessage());
                log.error("[OutboxPublisher] 이벤트 발행 실패 - id={}, error={}",
                        outbox.getEventId(),
                        ex.getMessage());
            }
        }
    }

    // 기존 EventPublisher 규칙과 동일하게 매핑
    private String resolveTopic(String eventType) {
        if (eventType.startsWith("Stock")) return "stock-events";
        if (eventType.startsWith("Order")) return "order-events";
        if (eventType.startsWith("Payment")) return "payment-events";
        return "default-events";
    }

}

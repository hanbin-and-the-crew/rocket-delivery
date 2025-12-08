package org.sparta.product.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.event.EventPublisher;
import org.sparta.product.domain.event.StockConfirmedEvent;
import org.sparta.product.domain.outbox.ProductOutboxEvent;
import org.sparta.product.domain.repository.ProductOutboxEventRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Outbox 테이블에서 READY 상태 이벤트를 읽어
 * 실제 Kafka 등 외부로 발행하는 Publisher.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductOutboxPublisher {

    private final ProductOutboxEventRepository outboxRepository;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    /**
     * 향후 @Scheduled 로 돌릴 수 있는 메서드.
     */
    @Transactional
    // @Scheduled(fixedDelay = 1000L)
    public void publishPendingEvents() {
        List<ProductOutboxEvent> events = outboxRepository.findReadyEvents(100);

        for (ProductOutboxEvent outbox : events) {
            try {
                if (!"STOCK_CONFIRMED".equals(outbox.getEventType())) {
                    log.warn("[ProductOutboxPublisher] 알 수 없는 이벤트 타입 - id={}, type={}",
                            outbox.getId(), outbox.getEventType());
                    outbox.markFailed();
                    outboxRepository.save(outbox);
                    continue;
                }

                StockConfirmedEvent event =
                        objectMapper.readValue(outbox.getPayload(), StockConfirmedEvent.class);

                eventPublisher.publishExternal(event);

                outbox.markSent();
                outboxRepository.save(outbox);

                log.info("[ProductOutboxPublisher] StockConfirmedEvent 발행 완료 - outboxId={}, orderId={}",
                        outbox.getId(), event.orderId());

            } catch (Exception ex) {
                log.error("[ProductOutboxPublisher] 이벤트 발행 실패 - outboxId={}", outbox.getId(), ex);
                outbox.markFailed();
                outboxRepository.save(outbox);
            }
        }
    }
}

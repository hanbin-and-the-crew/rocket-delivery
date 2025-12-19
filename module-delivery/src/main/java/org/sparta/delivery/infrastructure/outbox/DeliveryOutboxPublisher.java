package org.sparta.delivery.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.event.EventPublisher;
import org.sparta.common.event.delivery.DeliveryCreatedEvent;
import org.sparta.delivery.domain.entity.DeliveryOutboxEvent;
import org.sparta.delivery.domain.repository.DeliveryOutBoxEventRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryOutboxPublisher {

    private final DeliveryOutBoxEventRepository  deliveryOutboxRepository;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 2000) // 2초마다 체크, 부하 고려해서 주기를 늘려도 됨.
    @Transactional
    public void publishReadyEvents() {
        List<DeliveryOutboxEvent> events = deliveryOutboxRepository.findReadyEvents(100);

        for (DeliveryOutboxEvent outbox : events) {
            try {
                // 이벤트 타입에 따라 분기 처리
                switch (outbox.getEventType()) {
                    case "DeliveryCreatedEvent" -> {
                        DeliveryCreatedEvent event = objectMapper.readValue(
                                outbox.getPayload(),
                                DeliveryCreatedEvent.class
                        );
                        eventPublisher.publishExternal(event);
                        log.info("[DeliveryOutboxPublisher] DeliveryCreatedEvent 발행 완료 - outboxId={}, deliveryId={}",
                                outbox.getId(), event.deliveryId());
                    }
                    default -> {
                        log.warn("[DeliveryOutboxPublisher] 알 수 없는 이벤트 타입 - outboxId={}, eventType={}",
                                outbox.getId(), outbox.getEventType());
                        continue; // 다음 이벤트로 넘어감
                    }
                }

                // 발행 성공 시 SENT로 마킹
                outbox.markSent();
                deliveryOutboxRepository.save(outbox);

            } catch (Exception ex) {
                log.error("[DeliveryOutboxPublisher] 이벤트 발행 실패 - outboxId={}, eventType={}",
                        outbox.getId(), outbox.getEventType(), ex);

                outbox.increaseRetry();
                if (outbox.getRetryCount() >= 3) {
                    outbox.markFailed();
                    log.error("[DeliveryOutboxPublisher] 최대 재시도 초과 - outboxId={}", outbox.getId());
                }
                deliveryOutboxRepository.save(outbox);
            }
        }
    }
}

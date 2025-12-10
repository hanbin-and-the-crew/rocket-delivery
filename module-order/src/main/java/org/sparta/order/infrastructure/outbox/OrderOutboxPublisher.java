package org.sparta.order.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.event.EventPublisher;
import org.sparta.common.event.order.OrderCreatedEvent;
import org.sparta.order.domain.entity.OrderOutboxEvent;
import org.sparta.order.domain.repository.OrderOutboxEventRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderOutboxPublisher {

    private final OrderOutboxEventRepository outboxRepository;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 2000) // 2초마다 체크, 부하 고려해서 주기를 늘려도 됨.
    @Transactional
    public void publishReadyEvents() {

        List<OrderOutboxEvent> events =
                outboxRepository.findReadyEvents(100);

        for (OrderOutboxEvent outbox : events) {
            try {
                OrderCreatedEvent event =
                        objectMapper.readValue(outbox.getPayload(), OrderCreatedEvent.class);

                eventPublisher.publishExternal(event);

                outbox.markSent();
                outboxRepository.save(outbox);

                log.info("[OrderOutboxPublisher] OrderCreatedEvent 발행 완료 - outboxId={}, orderId={}",
                        outbox.getId(), event.orderId());

            } catch (Exception ex) {
                log.error("[OrderOutboxPublisher] 이벤트 발행 실패 - outboxId={}", outbox.getId(), ex);
                outbox.increaseRetry();
                if (outbox.getRetryCount() >= 3) {
                    outbox.markFailed();
                    log.error("[OrderOutboxPublisher] 최대 재시도 횟수 초과 - outboxId={}", outbox.getId());
                }
                outboxRepository.save(outbox);
            }
        }
    }
}
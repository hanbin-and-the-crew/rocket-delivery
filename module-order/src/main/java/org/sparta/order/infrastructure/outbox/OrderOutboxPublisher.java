package org.sparta.order.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.event.EventPublisher;
import org.sparta.common.event.order.OrderApprovedEvent;
import org.sparta.common.event.order.OrderCancelledEvent;
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
        List<OrderOutboxEvent> events = outboxRepository.findReadyEvents(100);

        for (OrderOutboxEvent outbox : events) {
            try {
                // 이벤트 타입에 따라 분기 처리
                switch (outbox.getEventType()) {
                    case "OrderCreatedEvent" -> {
                        OrderCreatedEvent event = objectMapper.readValue(
                                outbox.getPayload(),
                                OrderCreatedEvent.class
                        );
                        eventPublisher.publishExternal(event);
                        log.info("[OrderOutboxPublisher] OrderCreatedEvent 발행 완료 - outboxId={}, orderId={}",
                                outbox.getId(), event.orderId());
                    }
                    case "OrderApprovedEvent" -> {
                        OrderApprovedEvent event = objectMapper.readValue(
                                outbox.getPayload(),
                                OrderApprovedEvent.class
                        );
                        eventPublisher.publishExternal(event);
                        log.info("[OrderOutboxPublisher] OrderApprovedEvent 발행 완료 - outboxId={}, orderId={}",
                                outbox.getId(), event.orderId());
                    }
                    case "OrderCancelledEvent" -> {
                        OrderCancelledEvent event = objectMapper.readValue(
                                outbox.getPayload(),
                                OrderCancelledEvent.class
                        );
                        eventPublisher.publishExternal(event);
                        log.info("[OrderOutboxPublisher] OrderCancelledEvent 발행 완료 - outboxId={}, orderId={}",
                                outbox.getId(), event.orderId());
                    }
                    default -> {
                        log.warn("[OrderOutboxPublisher] 알 수 없는 이벤트 타입 - outboxId={}, eventType={}",
                                outbox.getId(), outbox.getEventType());
                        continue; // 다음 이벤트로 넘어감
                    }
                }

                // 발행 성공 시 SENT로 마킹
                outbox.markSent();
                outboxRepository.save(outbox);

            } catch (Exception ex) {
                log.error("[OrderOutboxPublisher] 이벤트 발행 실패 - outboxId={}, eventType={}",
                        outbox.getId(), outbox.getEventType(), ex);

                outbox.increaseRetry();
                if (outbox.getRetryCount() >= 3) {
                    outbox.markFailed();
                    log.error("[OrderOutboxPublisher] 최대 재시도 초과 - outboxId={}", outbox.getId());
                }
                outboxRepository.save(outbox);
            }
        }
    }
}

package org.sparta.product.infrastructure.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductOutboxScheduler {

    private final ProductOutboxPublisher publisher;

    // 1초마다 Outbox 발행 (조절 가능)
    @Scheduled(fixedDelay = 1000)
    public void publishOutboxEvents() {
        try {
            publisher.publishPendingEvents();
        } catch (Exception e) {
            log.error("[ProductOutboxScheduler] Outbox 발행 중 예외 발생", e);
        }
    }
}

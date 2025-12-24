package org.sparta.product.infrastructure.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.product.domain.outbox.OutboxStatus;
import org.sparta.product.domain.repository.ProductOutboxEventRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class ProductOutboxScheduler {

    private static final int MIN_READY_EVENTS_TO_PUBLISH = 3;   // 테스트 편하게 작은 수로 설정

    private final ProductOutboxPublisher publisher;
    private final ProductOutboxEventRepository outboxRepository;

    // 1초마다 체크는 하되, READY 이벤트가 어느 정도 쌓였을 때만 발행
    @Scheduled(fixedDelay = 15000)
    public void publishOutboxEvents() {
        try {
            long readyCount = outboxRepository.countByStatus(OutboxStatus.READY);

            // 임계치 미만이면 발행 스킵
            if (readyCount < MIN_READY_EVENTS_TO_PUBLISH) {
                log.debug(
                        "[ProductOutboxScheduler] READY Outbox 수({}) < 임계치({}) - 발행 스킵",
                        readyCount, MIN_READY_EVENTS_TO_PUBLISH
                );
                return;
            }

            log.info(
                    "[ProductOutboxScheduler] READY Outbox {}개 이상 감지 - 발행 시도 시작",
                    readyCount
            );

            publisher.publishPendingEvents();

        } catch (Exception e) {
            log.error("[ProductOutboxScheduler] Outbox 발행 중 예외 발생", e);
        }
    }
}

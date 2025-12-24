package org.sparta.product.infrastructure.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.redis.util.DistributedLockExecutor;
import org.sparta.redis.util.LockAcquisitionException;
import org.sparta.product.domain.outbox.ProductOutboxEvent;
import org.sparta.product.domain.repository.ProductOutboxEventRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Outbox 테이블에서 READY 상태 이벤트를 읽어 외부(Kafka)로 발행하는 오케스트레이터.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductOutboxPublisher {

    private final ProductOutboxEventRepository outboxRepository;
    private final DistributedLockExecutor lockExecutor;
    private final ProductOutboxSingleEventPublisher singleEventPublisher;

    public void publishPendingEvents() {
        List<ProductOutboxEvent> events = outboxRepository.findReadyEvents(100);

        for (ProductOutboxEvent outbox : events) {
            String lockKey = "product:outbox:lock:" + outbox.getId();

            try {
                // 경쟁 상황에서 전체 배치가 막히지 않게 waitTime=0 (즉시 스킵)
                lockExecutor.executeWithLock(
                        lockKey,
                        0,
                        15,
                        TimeUnit.SECONDS,
                        () -> {
                            try {
                                singleEventPublisher.publishSingleEvent(outbox);
                            } catch (Exception ex) {
                                log.error("[ProductOutboxPublisher] Outbox 발행 최종 실패 - FAIL 처리, id={}, type={}",
                                        outbox.getId(), outbox.getEventType(), ex);
                                outbox.markFailed();
                                outboxRepository.save(outbox);
                            }
                            return null;
                        }
                );
            } catch (LockAcquisitionException ex) {
                // 다른 인스턴스가 처리 중이면 스킵
                log.debug("[ProductOutboxPublisher] Outbox lock 획득 실패 - skip, id={}, type={}",
                        outbox.getId(), outbox.getEventType());
            } catch (Exception ex) {
                log.error("[ProductOutboxPublisher] Outbox 처리 중 예외 - FAIL 처리, id={}, type={}",
                        outbox.getId(), outbox.getEventType(), ex);
                outbox.markFailed();
                outboxRepository.save(outbox);
            }
        }
    }
}

package org.sparta.product.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.event.EventPublisher;
import org.sparta.common.event.product.StockConfirmedEvent;
import org.sparta.product.domain.outbox.ProductOutboxEvent;
import org.sparta.product.domain.repository.ProductOutboxEventRepository;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;

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
     * Product Outbox 발행용 서킷브레이커
     * - 실패율 50% 이상이면 OPEN
     * - 최근 20개 호출 기준으로 판단
     * - OPEN 상태는 10초 유지 후 HALF_OPEN
     * - HALF_OPEN 에서 3개 시도 성공하면 다시 CLOSED
     */
    private final CircuitBreaker outboxCircuitBreaker = CircuitBreaker.of(
            "product-outbox",
            CircuitBreakerConfig.custom()
                    .failureRateThreshold(50.0f)                // 실패율 50% 이상이면 OPEN
                    .slidingWindowSize(20)                       // 최근 20개 호출 기준
                    .waitDurationInOpenState(Duration.ofSeconds(10)) // OPEN 상태 유지 시간 10 초
                    .permittedNumberOfCallsInHalfOpenState(3)    // HALF_OPEN 시 허용 호출 수 3 건
                    .build()
    );

    @Transactional
    public void publishPendingEvents() {
        List<ProductOutboxEvent> events = outboxRepository.findReadyEvents(100);

        for (ProductOutboxEvent outbox : events) {

            // 1) 이벤트 타입 먼저 확인
            if (!"STOCK_CONFIRMED".equals(outbox.getEventType())) {
                log.warn("[ProductOutboxPublisher] 알 수 없는 이벤트 타입 - id={}, type={}",
                        outbox.getId(), outbox.getEventType());

                outbox.markFailed();
                outboxRepository.save(outbox);
                continue;
            }

            try {
                // 여기서 publishSingleEvent 가 최대 3번까지 재시도
                publishSingleEvent(outbox);

            } catch (Exception ex) {
                // 재시도 모두 실패한 경우 여기로 들어옴
                log.error("[ProductOutboxPublisher] Outbox 최종 발행 실패 - FAIL 처리, id={}", outbox.getId(), ex);

                outbox.markFailed();
                outboxRepository.save(outbox);
            }
        }
    }

    @Retryable(
            retryFor = { Exception.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, maxDelay = 2000, multiplier = 2)
    )
    @Transactional
    public void publishSingleEvent(ProductOutboxEvent outbox) throws Exception {

        // 1) payload 역직렬화
        StockConfirmedEvent event =
                objectMapper.readValue(outbox.getPayload(), StockConfirmedEvent.class);

        // 2) 외부 발행 + 서킷브레이커 적용
        try {
            // 서킷이 CLOSED/HALF_OPEN 상태면 내부에서 publishExternal 실행
            // 서킷이 OPEN 상태면 CallNotPermittedException 을 즉시 던져서 fast-fail
            outboxCircuitBreaker.executeRunnable(() -> eventPublisher.publishExternal(event));

        } catch (Exception ex) {
            // 여기서 예외는 Retry 에 의해 재시도 대상이 되고,
            // 최종적으로는 publishPendingEvents() 의 catch 로 전파된다.
            log.error("[ProductOutboxPublisher] 외부 발행 실패 - outboxId={}, circuitState={}",
                    outbox.getId(), outboxCircuitBreaker.getState(), ex);
            throw ex;
        }

        // 3) 성공 시 상태 변경
        outbox.markSent();
        outboxRepository.save(outbox);

        log.info("[ProductOutboxPublisher] Outbox event 발행 성공 - id={}", outbox.getId());
    }

}

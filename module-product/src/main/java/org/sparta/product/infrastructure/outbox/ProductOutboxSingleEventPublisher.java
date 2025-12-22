package org.sparta.product.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.event.DomainEvent;
import org.sparta.common.event.EventPublisher;
import org.sparta.common.event.product.StockConfirmedEvent;
import org.sparta.common.event.product.StockReservationFailedEvent;
import org.sparta.product.domain.outbox.ProductOutboxEvent;
import org.sparta.product.domain.repository.ProductOutboxEventRepository;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * Outbox 이벤트 1건을 외부로 발행하는 전용 컴포넌트.
 *
 * 중요:
 * - @Retryable은 프록시 기반이라 self-invocation(자기 자신 메서드 호출)에서는 동작하지 않는다.
 * - 그래서 단건 발행 로직을 별도 Bean으로 분리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductOutboxSingleEventPublisher {

    private final ProductOutboxEventRepository outboxRepository;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    private final CircuitBreaker outboxCircuitBreaker = CircuitBreaker.of(
            "product-outbox",
            CircuitBreakerConfig.custom()
                    .failureRateThreshold(50.0f)
                    .slidingWindowSize(20)
                    .waitDurationInOpenState(Duration.ofSeconds(10))
                    .permittedNumberOfCallsInHalfOpenState(3)
                    .build()
    );

    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, maxDelay = 2000, multiplier = 2)
    )
    @Transactional
    public void publishSingleEvent(ProductOutboxEvent outbox) throws Exception {
        DomainEvent event = deserialize(outbox);

        try {
            outboxCircuitBreaker.executeRunnable(() -> eventPublisher.publishExternal(event));
        } catch (Exception ex) {
            log.error("[ProductOutboxSingleEventPublisher] 외부 발행 실패 - outboxId={}, eventType={}, circuitState={}",
                    outbox.getId(), outbox.getEventType(), outboxCircuitBreaker.getState(), ex);
            throw ex;
        }

        outbox.markSent();
        outboxRepository.save(outbox);

        log.info("[ProductOutboxSingleEventPublisher] Outbox event 발행 성공 - id={}, type={}",
                outbox.getId(), outbox.getEventType());
    }

    private DomainEvent deserialize(ProductOutboxEvent outbox) throws Exception {
        return switch (outbox.getEventType()) {
            case "STOCK_CONFIRMED" ->
                    objectMapper.readValue(outbox.getPayload(), StockConfirmedEvent.class);
            case "STOCK_RESERVATION_FAILED" ->
                    objectMapper.readValue(outbox.getPayload(), StockReservationFailedEvent.class);
            default -> throw new IllegalArgumentException("Unsupported outbox eventType: " + outbox.getEventType());
        };
    }
}

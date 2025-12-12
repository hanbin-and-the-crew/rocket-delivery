package org.sparta.product.localtest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.common.event.EventPublisher;
import org.sparta.common.event.product.StockConfirmedEvent;
import org.sparta.product.domain.outbox.ProductOutboxEvent;
import org.sparta.product.domain.repository.ProductOutboxEventRepository;
import org.sparta.product.infrastructure.outbox.ProductOutboxPublisher;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * OutboxPublisher + CircuitBreaker 동작 확인
 *
 * - STOCK_CONFIRMED Outbox 이벤트를 외부로 발행하려고 할 때
 * - 외부 시스템(예: Kafka)이 계속 장애라고 가정하고 publishExternal 이 계속 예외를 던지면
 * - 일정 횟수 이상 실패 후에는 CircuitBreaker 가 OPEN 상태가 되어
 *   이후 호출에서 publishExternal 이 아예 호출되지(=fast-fail) 않는지 검증한다.
 */
class OutboxPublisherResilienceTest {

    private ProductOutboxEventRepository outboxRepository; // save() 호출 여부
    private EventPublisher eventPublisher;                  // 외부 발행 모킹 (Kafka 대역)
    private ObjectMapper objectMapper;

    private ProductOutboxPublisher publisher;               // 실제 테스트 대상

    @BeforeEach
    void setUp() {
        outboxRepository = mock(ProductOutboxEventRepository.class);
        eventPublisher = mock(EventPublisher.class);

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        publisher = new ProductOutboxPublisher(
                outboxRepository,
                eventPublisher,
                objectMapper
        );
    }

    @Test
    @DisplayName("외부 발행 실패가 누적되면 CircuitBreaker가 열리고 fast-fail로 전환된다")
    void circuitBreakerOpensAndFastFailsWhenExternalPublishKeepsFailing() throws Exception {
        // given
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        int quantity = 5;

        // 도메인 이벤트 생성
        StockConfirmedEvent domainEvent = StockConfirmedEvent.of(orderId, productId, quantity);

        // Outbox payload 로 사용할 JSON
        String payloadJson = objectMapper.writeValueAsString(domainEvent);

        // READY 상태의 STOCK_CONFIRMED Outbox 이벤트 생성
        ProductOutboxEvent outbox = ProductOutboxEvent.stockConfirmed(domainEvent, payloadJson);

        // 외부 발행이 항상 실패한다고 가정 (Kafka 장애 상황 모킹)
        doThrow(new RuntimeException("Kafka down"))
                .when(eventPublisher)
                .publishExternal(any(StockConfirmedEvent.class));

        // when 1) 여러 번 발행 시도 → CircuitBreaker 에 실패 누적
        // Resilience4j 기본 설정에서 minimumNumberOfCalls 가 100이기 때문에,
        // 안전하게 120번 정도 반복 호출해서 실패율 통계를 충분히 쌓아준다.
        for (int i = 0; i < 120; i++) {
            try {
                publisher.publishSingleEvent(outbox);
            } catch (Exception ignored) {
                // 예외는 무시하고 실패만 누적시킨다.
            }
        }

        // 이전 호출 기록은 모두 지우고, 마지막 한 번만 관찰
        reset(eventPublisher);

        // when 2) CircuitBreaker 가 OPEN 상태일 때 다시 한 번 시도
        try {
            publisher.publishSingleEvent(outbox);
        } catch (Exception ignored) {
            // 이때는 CircuitBreaker 가 CallNotPermittedException 을 던질 수 있지만
            // 우리는 publishExternal 이 호출되지 않는지만 확인하면 된다.
        }

        // then
        // CircuitBreaker 가 열려 있다면, 내부에서 publishExternal() 는 호출되지 않는다.
        verify(eventPublisher, times(0)).publishExternal(any());
    }
}

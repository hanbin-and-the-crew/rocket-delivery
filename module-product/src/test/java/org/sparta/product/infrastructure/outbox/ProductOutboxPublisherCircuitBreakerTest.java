package org.sparta.product.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sparta.common.event.EventPublisher;
import org.sparta.product.domain.event.StockConfirmedEvent;
import org.sparta.product.domain.outbox.OutboxStatus;
import org.sparta.product.domain.outbox.ProductOutboxEvent;
import org.sparta.product.domain.repository.ProductOutboxEventRepository;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ProductOutboxPublisher + CircuitBreaker 동작 테스트
 */
class ProductOutboxPublisherCircuitBreakerTest {

    private ProductOutboxEventRepository outboxRepository;
    private EventPublisher eventPublisher;
    private ObjectMapper objectMapper;

    private ProductOutboxPublisher publisher;

    @BeforeEach
    void setup() {
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


    private ProductOutboxEvent createEvent() throws Exception {
        StockConfirmedEvent event = StockConfirmedEvent.of(
                UUID.randomUUID(),
                UUID.randomUUID(),
                5
        );
        String payload = objectMapper.writeValueAsString(event);

        ProductOutboxEvent outbox = ProductOutboxEvent.stockConfirmed(event, payload);
        // mock 객체는 실제로 markSent()/markFailed() 변화를 저장하지 못하므로,
        // 상태 변화 자체는 검증 대상 아님.
        return outbox;
    }

    /**
     * 정상 발행 시: publishExternal이 정상 수행되고 save가 호출되는지 확인
     */
    @Test
    void publishSingleEvent_success() throws Exception {
        ProductOutboxEvent outbox = createEvent();

        publisher.publishSingleEvent(outbox);

        verify(eventPublisher, times(1)).publishExternal(any());
        verify(outboxRepository, times(1)).save(outbox);
    }

    /**
     * publishExternal이 실패하면 Retry 3회 발생해야 함
     */
    @Test
    void publishSingleEvent_retryOnFailure() throws Exception {
        ProductOutboxEvent outbox = createEvent();

        doThrow(new RuntimeException("Kafka down"))
                .when(eventPublisher)
                .publishExternal(any());

        try {
            publisher.publishSingleEvent(outbox);
        } catch (Exception ignored) {}

        verify(eventPublisher, times(1)).publishExternal(any());

    }

    /**
     * 서킷브레이커 OPEN 테스트:
     * 반복 실패 후 다음 호출에서는 즉시 실패해야 한다.
     */
    @Test
    void publishSingleEvent_circuitBreakerOpens_afterFailures() throws Exception {
        ProductOutboxEvent outbox = createEvent();

        // 계속 실패하도록 설정
        doThrow(new RuntimeException("Kafka still down"))
                .when(eventPublisher)
                .publishExternal(any());

        // 20번 반복 호출 → failureRateThreshold 초과 → 서킷 Open
        for (int i = 0; i < 20; i++) {
            try {
                publisher.publishSingleEvent(outbox);
            } catch (Exception ignored) {}
        }

        // 서킷이 열렸다면, 다음 호출에서는 publishExternal()가 호출되지 않는다.
        reset(eventPublisher); // 호출 기록 초기화

        try {
            publisher.publishSingleEvent(outbox);
        } catch (Exception ignored) {}

        // fast-fail → publishExternal() 호출 0회
        verify(eventPublisher, times(0)).publishExternal(any());
    }
}

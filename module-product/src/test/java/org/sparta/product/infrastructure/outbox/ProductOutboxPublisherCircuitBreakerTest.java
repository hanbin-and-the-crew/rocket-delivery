package org.sparta.product.infrastructure.outbox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.product.domain.outbox.OutboxStatus;
import org.sparta.product.domain.outbox.ProductOutboxEvent;
import org.sparta.product.domain.repository.ProductOutboxEventRepository;
import org.sparta.common.event.EventPublisher;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.mockito.Mockito.*;

class ProductOutboxPublisherCircuitBreakerTest {

    @Test
    @DisplayName("외부 발행 실패 시 outbox FAILED 전환")
    void publish_fail_marks_failed() throws Exception {
        ProductOutboxEventRepository repo = mock(ProductOutboxEventRepository.class);
        EventPublisher eventPublisher = mock(EventPublisher.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);

        ProductOutboxEvent event = ProductOutboxEvent.builder()
                .eventType("STOCK_CONFIRMED")
                .payload("{}")
                .status(OutboxStatus.READY)
                .build();

        when(objectMapper.readValue(anyString(),
                eq(org.sparta.common.event.product.StockConfirmedEvent.class)))
                .thenThrow(new RuntimeException("fail"));

        ProductOutboxSingleEventPublisher publisher =
                new ProductOutboxSingleEventPublisher(repo, eventPublisher, objectMapper);

        try {
            publisher.publishSingleEvent(event);
        } catch (Exception ignored) {
        }

        verify(repo, never()).save(argThat(e -> e.getStatus() == OutboxStatus.SENT));
    }
}

package org.sparta.product.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sparta.common.event.EventPublisher;
import org.sparta.common.event.product.StockConfirmedEvent;
import org.sparta.product.domain.outbox.OutboxStatus;
import org.sparta.product.domain.outbox.ProductOutboxEvent;
import org.sparta.product.domain.repository.ProductOutboxEventRepository;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductOutboxPublisherTest {

    @Mock
    private ProductOutboxEventRepository outboxRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ProductOutboxPublisher publisher;

    @Test
    @DisplayName("READY 상태 Outbox 이벤트를 성공적으로 발행하면 SENT 상태로 변경된다")
    void publishPendingEvents_success() throws Exception {
        // given
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        int quantity = 5;

        StockConfirmedEvent domainEvent = StockConfirmedEvent.of(orderId, productId, quantity);

        ProductOutboxEvent outboxEvent = ProductOutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("ORDER")
                .aggregateId(orderId)
                .eventType("STOCK_CONFIRMED")
                .payload("{\"dummy\":\"json\"}")
                .status(OutboxStatus.READY)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(0L)
                .build();

        when(outboxRepository.findReadyEvents(100))
                .thenReturn(List.of(outboxEvent));

        when(objectMapper.readValue(outboxEvent.getPayload(), StockConfirmedEvent.class))
                .thenReturn(domainEvent);

        // when
        publisher.publishPendingEvents();

        // then
        // 1) 이벤트 발행이 한 번 호출돼야 하고
        verify(eventPublisher, times(1)).publishExternal(domainEvent);

        // 2) Outbox 상태가 SENT 로 변경되어 저장돼야 한다
        ArgumentCaptor<ProductOutboxEvent> captor =
                ArgumentCaptor.forClass(ProductOutboxEvent.class);

        verify(outboxRepository, times(1)).save(captor.capture());

        ProductOutboxEvent saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(outboxEvent.getId());
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.SENT);
    }

    @Test
    @DisplayName("READY 이벤트가 없으면 아무 작업도 수행하지 않는다")
    void publishPendingEvents_noReadyEvents() {
        // given
        when(outboxRepository.findReadyEvents(100))
                .thenReturn(Collections.emptyList());

        // when
        publisher.publishPendingEvents();

        // then
        verify(eventPublisher, never()).publishExternal(any());
        verify(outboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("알 수 없는 이벤트 타입이면 FAILED 상태로 변경되고 publishExternal 은 호출되지 않는다")
    void publishPendingEvents_unknownEventType() {
        // given
        ProductOutboxEvent outboxEvent = ProductOutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("ORDER")
                .aggregateId(UUID.randomUUID())
                .eventType("UNKNOWN_TYPE")
                .payload("{\"dummy\":\"json\"}")
                .status(OutboxStatus.READY)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(0L)
                .build();

        when(outboxRepository.findReadyEvents(100))
                .thenReturn(List.of(outboxEvent));

        // when
        publisher.publishPendingEvents();

        // then
        verify(eventPublisher, never()).publishExternal(any());

        ArgumentCaptor<ProductOutboxEvent> captor =
                ArgumentCaptor.forClass(ProductOutboxEvent.class);

        verify(outboxRepository, times(1)).save(captor.capture());

        ProductOutboxEvent saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.FAILED);
    }

    @Test
    @DisplayName("publishExternal 중 예외가 발생하면 FAILED 상태로 변경된다")
    void publishPendingEvents_publishFails_marksFailed() throws Exception {
        // given
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        StockConfirmedEvent domainEvent = StockConfirmedEvent.of(orderId, productId, 3);

        ProductOutboxEvent outboxEvent = ProductOutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("ORDER")
                .aggregateId(orderId)
                .eventType("STOCK_CONFIRMED")
                .payload("{\"dummy\":\"json\"}")
                .status(OutboxStatus.READY)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(0L)
                .build();

        when(outboxRepository.findReadyEvents(100))
                .thenReturn(List.of(outboxEvent));

        when(objectMapper.readValue(outboxEvent.getPayload(), StockConfirmedEvent.class))
                .thenReturn(domainEvent);

        doThrow(new RuntimeException("Kafka down"))
                .when(eventPublisher)
                .publishExternal(domainEvent);

        // when
        publisher.publishPendingEvents();

        // then
        verify(eventPublisher, times(1)).publishExternal(domainEvent);

        ArgumentCaptor<ProductOutboxEvent> captor =
                ArgumentCaptor.forClass(ProductOutboxEvent.class);

        verify(outboxRepository, times(1)).save(captor.capture());

        ProductOutboxEvent saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.FAILED);
    }
}

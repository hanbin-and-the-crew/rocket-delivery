package org.sparta.product.infrastructure.outbox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.product.domain.outbox.OutboxStatus;
import org.sparta.product.domain.outbox.ProductOutboxEvent;
import org.sparta.product.domain.repository.ProductOutboxEventRepository;
import org.sparta.redis.util.DistributedLockExecutor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProductOutboxPublisherTest {

    @Test
    @DisplayName("READY outbox 조회 후, 락을 통해 단건 발행 로직을 실행하고 publisher에 위임한다")
    void publish_ready_events() throws Exception {
        ProductOutboxEventRepository outboxRepository = mock(ProductOutboxEventRepository.class);
        DistributedLockExecutor lockExecutor = mock(DistributedLockExecutor.class);
        ProductOutboxSingleEventPublisher singlePublisher = mock(ProductOutboxSingleEventPublisher.class);

        ProductOutboxEvent outbox = ProductOutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("ORDER")
                .aggregateId(UUID.randomUUID())
                .eventType("STOCK_CONFIRMED")
                .payload("{}")
                .status(OutboxStatus.READY)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(outboxRepository.findReadyEvents(100)).thenReturn(List.of(outbox));

        // DistributedLockExecutor 시그니처는 Supplier<T> 기반이므로 Supplier로 모킹
        when(lockExecutor.executeWithLock(
                anyString(),
                anyLong(),
                anyLong(),
                any(TimeUnit.class),
                any(Supplier.class)
        )).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Supplier<Object> supplier = (Supplier<Object>) invocation.getArgument(4);
            return supplier.get();
        });

        // checked exception 시그니처 존중
        doNothing().when(singlePublisher).publishSingleEvent(outbox);

        ProductOutboxPublisher publisher =
                new ProductOutboxPublisher(outboxRepository, lockExecutor, singlePublisher);

        publisher.publishPendingEvents();

        verify(outboxRepository, times(1)).findReadyEvents(100);
        verify(singlePublisher, times(1)).publishSingleEvent(outbox);
        verify(outboxRepository, never()).save(any()); // 성공 케이스에서 FAIL 저장은 없어야 함
    }
}


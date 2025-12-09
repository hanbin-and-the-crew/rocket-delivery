package org.sparta.payment.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sparta.payment.domain.entity.PaymentOutbox;
import org.sparta.payment.domain.enumeration.OutboxStatus;
import org.sparta.payment.domain.repository.PaymentOutboxRepository;
import org.sparta.payment.infrastructure.outbox.PaymentOutboxPublisher;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * PaymentOutboxPublisher.publishReadyEvents() 에 대한 순수 유닛 테스트.
 * - 실제 Kafka, DB, Scheduler 는 사용하지 않고, Mockito 로만 동작을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class PaymentOutboxPublisherTest {

    @Mock
    private PaymentOutboxRepository outboxRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PaymentOutboxPublisher publisher;

    @Test
    @DisplayName("publishReadyEvents - READY 상태 Outbox를 Kafka로 발행하고 SENT로 마킹한다")
    void publishReadyEvents_success() {
        // given
        PaymentOutbox event = mock(PaymentOutbox.class);
        UUID aggregateId = UUID.randomUUID();

        when(outboxRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.READY))
                .thenReturn(List.of(event));

        when(event.getAggregateId()).thenReturn(aggregateId);
        when(event.getPayload()).thenReturn("{\"dummy\":\"event\"}");

        // KafkaTemplate.send(...) 가 이미 성공 완료된 CompletableFuture 를 리턴하도록 설정
        CompletableFuture<RecordMetadata> successFuture = new CompletableFuture<>();
        successFuture.complete(null); // 결과 자체는 지금 테스트에서 중요하지 않음

        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn((CompletableFuture) successFuture);

        // when
        publisher.publishReadyEvents();

        // then
        // Kafka send 호출 검증
        verify(kafkaTemplate, times(1))
                .send(eq("payment-events"), eq(aggregateId.toString()), eq("{\"dummy\":\"event\"}"));

        // 성공 시 markSent() 가 호출되고, increaseRetry / markFailed 는 호출되지 않아야 한다.
        verify(event, times(1)).markSent();
        verify(event, never()).increaseRetry();
        verify(event, never()).markFailed();

        // 최종적으로 outboxRepository.save(event) 가 호출되어야 한다.
        verify(outboxRepository, times(1)).save(event);
    }

    @Test
    @DisplayName("publishReadyEvents - Kafka 전송 실패 시 retry 증가 및 저장이 호출된다")
    void publishReadyEvents_failure() {
        // given
        PaymentOutbox event = mock(PaymentOutbox.class);
        UUID aggregateId = UUID.randomUUID();

        when(outboxRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.READY))
                .thenReturn(List.of(event));

        when(event.getAggregateId()).thenReturn(aggregateId);
        when(event.getPayload()).thenReturn("{\"dummy\":\"event\"}");
        when(event.getRetryCount()).thenReturn(0); // 아직 최대 재시도에 도달하지 않은 상태라고 가정

        // 실패한 CompletableFuture 생성
        CompletableFuture<RecordMetadata> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka send failed"));

        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn((CompletableFuture) failedFuture);

        // when
        publisher.publishReadyEvents();

        // then
        // 실패 시 markSent() 는 호출되지 않고, increaseRetry() 는 호출되어야 한다.
        verify(event, never()).markSent();
        verify(event, times(1)).increaseRetry();

        // retryCount 가 MAX_RETRY 에 도달하지 않았다고 가정했으므로 markFailed() 는 호출되지 않을 수 있다.
        verify(event, never()).markFailed();

        // 어쨌든 이벤트 상태는 저장되어야 한다.
        verify(outboxRepository, times(1)).save(event);
    }
}

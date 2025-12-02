package org.sparta.product.infrastructure.event.kafka;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.product.domain.enums.OutboxStatus;
import org.sparta.product.domain.event.ProductOutboxEvent;
import org.sparta.product.infrastructure.event.kafka.publisher.ProductOutboxPublisher;
import org.sparta.product.infrastructure.event.outbox.ProductOutboxEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductOutboxPublisherTest {

    @Autowired
    private ProductOutboxEventRepository productOutboxEventRepository;

    @Autowired
    private ProductOutboxPublisher productOutboxPublisher;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    @DisplayName("READY 상태 Outbox 이벤트를 Kafka로 전송하고 상태를 PUBLISHED로 변경한다")
    void publishOutboxEvents_ShouldSendToKafkaAndMarkPublished() {
        // given: READY 상태 Outbox 하나 생성
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();

        ProductOutboxEvent outbox = ProductOutboxEvent.builder()
                .eventId(eventId)
                .eventType("StockReservedEvent")
                .aggregateId(aggregateId)
                .payload("{\"dummy\":\"payload\"}")
                .status(OutboxStatus.READY)
                .occurredAt(Instant.now())
                .build();

        outbox = productOutboxEventRepository.save(outbox);

        // when: Outbox 발행 메서드 실행
        productOutboxPublisher.publishOutboxEvents();

        // then: KafkaTemplate.send(...) 호출됨
        verify(kafkaTemplate).send(anyString(), anyString());

        // then: Outbox 상태가 PUBLISHED로 변경
        ProductOutboxEvent updated = productOutboxEventRepository.findById(outbox.getId())
                .orElseThrow();

        assertThat(updated.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(updated.getPublishedAt()).isNotNull();
        assertThat(updated.getErrorMessage()).isNull();
    }
}

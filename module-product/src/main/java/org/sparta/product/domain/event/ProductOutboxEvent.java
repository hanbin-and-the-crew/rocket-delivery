package org.sparta.product.domain.event;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.product.domain.enums.OutboxStatus;

import java.time.Instant;
import java.util.UUID;


@Table(name = "p_product_outbox_event")
@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductOutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Kafka 발행을 식별하는 고유한 이벤트 ID
    @Column(nullable = false, unique = true)
    private UUID eventId;

    // 이벤트 종류 (예: StockReservedEvent)
    @Column(nullable = false)
    private String eventType;

    // 관련 애그리거트 ID (예: productId 또는 orderId)
    @Column(nullable = false)
    private UUID aggregateId;

    // 이벤트 JSON (직렬화된 payload)
    @Lob
    @Column(nullable = false)
    private String payload;

    // READY → PUBLISHED or FAILED
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    // 비즈니스에서 이벤트가 "발생한 시각"
    @Column(nullable = false)
    private Instant occurredAt;

    // Publisher가 실제 Kafka에 발행한 시각
    private Instant publishedAt;

    // 실패 시 에러 메시지 적재
    private String errorMessage;

    public void markPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = Instant.now();
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.status = OutboxStatus.FAILED;
        this.errorMessage = errorMessage;
    }
}

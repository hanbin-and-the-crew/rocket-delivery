package org.sparta.product.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.jpa.entity.BaseEntity;

import java.time.Instant;
import java.util.UUID;

/**
 * 처리된 이벤트 기록
 *
 * 멱등성 보장:
 * - 동일한 eventId의 이벤트를 중복 처리하지 않음
 * - Kafka 재시도, 네트워크 지연 등으로 인한 중복 이벤트 방지
 */
@Entity
@Getter
@Table(name = "p_processed_events",
        indexes = @Index(name = "idx_event_id", columnList = "eventId", unique = true))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID eventId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private Instant processedAt;

    private ProcessedEvent(UUID eventId, String eventType) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.processedAt = Instant.now();
    }

    public static ProcessedEvent of(UUID eventId, String eventType) {
        return new ProcessedEvent(eventId, eventType);
    }
}
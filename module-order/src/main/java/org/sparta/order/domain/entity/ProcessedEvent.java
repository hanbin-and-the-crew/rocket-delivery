package org.sparta.order.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
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
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    private ProcessedEvent(UUID eventId, String eventType) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.processedAt = LocalDateTime.now();
    }

    public static ProcessedEvent of(UUID eventId, String eventType) {
        return new ProcessedEvent(eventId, eventType);
    }
}
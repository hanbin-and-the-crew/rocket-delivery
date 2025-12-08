package org.sparta.user.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 처리된 이벤트 기록
 * - Kafka 이벤트 멱등성 보장을 위한 엔티티
 */
@Entity
@Table(name = "p_point_processed_events")
@Getter
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
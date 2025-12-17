package org.sparta.product.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 처리된 이벤트 기록
 * - Kafka 이벤트 멱등성 보장을 위한 엔티티
 * - product_db 의 p_processed_events 테이블과 매핑
 */
@Entity
@Table(
        name = "p_processed_events",
        indexes = {
                @Index(name = "idx_product_event_id", columnList = "event_id", unique = true)
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * 중복 처리 방지 키
     * - upstream 이벤트의 eventId(UUID)를 문자열로 저장
     */
    @Column(name = "event_id", nullable = false, unique = true, length = 255)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    private ProcessedEvent(String eventId, String eventType) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.processedAt = LocalDateTime.now();
    }

    public static ProcessedEvent of(String eventId, String eventType) {
        return new ProcessedEvent(eventId, eventType);
    }
}

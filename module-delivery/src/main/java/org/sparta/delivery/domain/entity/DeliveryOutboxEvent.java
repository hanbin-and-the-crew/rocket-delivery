package org.sparta.delivery.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.delivery.domain.enumeration.OutboxStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_delivery_outbox_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryOutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId; // deliveryId

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType; // DeliveryCreatedEvent

    @Lob
    @Column(name = "payload", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static DeliveryOutboxEvent ready(
            String aggregateType,
            UUID aggregateId,
            String eventType,
            String payload
    ) {
        DeliveryOutboxEvent e = new DeliveryOutboxEvent();
        e.aggregateType = aggregateType;
        e.aggregateId = aggregateId;
        e.eventType = eventType;
        e.payload = payload;
        e.status = OutboxStatus.READY;
        e.retryCount = 0;
        e.createdAt = LocalDateTime.now();
        return e;
    }

    public void markSent() {
        this.status = OutboxStatus.SENT;
        this.publishedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.status = OutboxStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    public void increaseRetry() {
        this.retryCount++;
        this.updatedAt = LocalDateTime.now();
    }
}

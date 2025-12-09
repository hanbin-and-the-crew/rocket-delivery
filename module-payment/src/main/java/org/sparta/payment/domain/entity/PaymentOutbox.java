package org.sparta.payment.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.payment.domain.enumeration.OutboxStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_payment_outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payment_outbox_id", nullable = false, updatable = false)
    private UUID paymentOutboxId;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

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

    public static PaymentOutbox ready(String aggregateType,
                                      UUID aggregateId,
                                      String eventType,
                                      String payload) {
        PaymentOutbox o = new PaymentOutbox();
        o.aggregateType = aggregateType;
        o.aggregateId = aggregateId;
        o.eventType = eventType;
        o.payload = payload;
        o.status = OutboxStatus.READY;
        o.retryCount = 0;
        o.createdAt = LocalDateTime.now();
        return o;
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

    public static PaymentOutbox create(String aggregateType,
                                       java.util.UUID aggregateId,
                                       String eventType,
                                       String payload,
                                       OutboxStatus status) {
        PaymentOutbox outbox = new PaymentOutbox();
        outbox.aggregateType = aggregateType;
        outbox.aggregateId = aggregateId;
        outbox.eventType = eventType;
        outbox.payload = payload;
        outbox.status = status;
        return outbox;
    }
}

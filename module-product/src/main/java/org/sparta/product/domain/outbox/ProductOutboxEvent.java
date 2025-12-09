package org.sparta.product.domain.outbox;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.product.domain.event.StockConfirmedEvent;
import org.sparta.product.domain.event.StockConfirmedEvent;
import org.sparta.product.domain.event.StockReservationFailedEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Product 모듈의 Outbox 이벤트 엔티티
 * - 재고 확정(StockConfirmedEvent) 등의 도메인 이벤트를
 *   DB에 먼저 저장한 뒤, 별도 Publisher 가 Kafka 등으로 발행한다.
 */
@Entity
@Table(name = "product_outbox_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ProductOutboxEvent {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * 이벤트가 속한 애그리게잇 종류
     * ex) "ORDER", "PRODUCT" 등
     */
    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    /**
     * 애그리게잇 식별자
     * ex) orderId, productId 등
     */
    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    /**
     * 이벤트 타입
     */
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    /**
     * 직렬화된 이벤트 payload (JSON)
     */
    @Lob
    @Column(name = "payload", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboxStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * StockConfirmedEvent 를 Outbox 이벤트로 감싸는 팩토리 메서드
     */
    public static ProductOutboxEvent stockConfirmed(StockConfirmedEvent event, String payloadJson) {
        Instant now = Instant.now();
        return ProductOutboxEvent.builder()
                .aggregateType("ORDER")
                .aggregateId(event.orderId())
                .eventType("STOCK_CONFIRMED")
                .payload(payloadJson)
                .status(OutboxStatus.READY)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /**
     * StockReservationFailedEvent 를 Outbox 이벤트로 감싸는 팩토리 메서드
     */
    public static ProductOutboxEvent stockReservationFailed(StockReservationFailedEvent event, String payloadJson) {
        Instant now = Instant.now();
        return ProductOutboxEvent.builder()
                .aggregateType("ORDER")
                .aggregateId(event.orderId())
                .eventType("STOCK_RESERVATION_FAILED")
                .payload(payloadJson)
                .status(OutboxStatus.READY)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }


    public void markSent() {
        this.status = OutboxStatus.SENT;
        this.updatedAt = Instant.now();
    }

    public void markFailed() {
        this.status = OutboxStatus.FAILED;
        this.updatedAt = Instant.now();
    }
}

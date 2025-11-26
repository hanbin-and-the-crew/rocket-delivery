package org.sparta.product.domain.event;

import org.sparta.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * 상품 삭제 이벤트
 *
 * Product Aggregate에서 발행:
 * - Product가 논리적으로 삭제될 때 발행
 * - Stock 모듈에서 수신하여 재고를 판매 불가 상태로 변경
 */
public record ProductDeletedEvent(
        UUID eventId,
        UUID productId,
        Instant occurredAt
) implements DomainEvent {

    public static ProductDeletedEvent of(UUID productId) {
        return new ProductDeletedEvent(
                UUID.randomUUID(),
                productId,
                Instant.now()
        );
    }
}
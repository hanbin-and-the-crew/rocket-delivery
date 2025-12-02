package org.sparta.product.domain.event;

import org.sparta.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * 상품 생성 이벤트
 *
 * Product Aggregate에서 발행:
 * - Product가 생성될 때 발행
 * - Stock 모듈에서 수신하여 재고 엔티티 생성
 */
public record ProductCreatedEvent(
        UUID eventId,
        UUID productId,
        UUID companyId,
        UUID hubId,
        Integer initialQuantity,
        Instant occurredAt
) implements DomainEvent {

    public static ProductCreatedEvent of(
            UUID productId,
            UUID companyId,
            UUID hubId,
            Integer initialQuantity
    ) {
        return new ProductCreatedEvent(
                UUID.randomUUID(),
                productId,
                companyId,
                hubId,
                initialQuantity,
                Instant.now()
        );
    }
}
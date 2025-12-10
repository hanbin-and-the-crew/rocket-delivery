package org.sparta.common.event.order;

import org.sparta.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * order event 발행
 * 사용자의 주문 취소 Kafka 이벤트
 * 파일 이름은 product에 있는 것과 동일하게 맞춤
 */
public record OrderCanceledEvent(
        UUID eventId,
        UUID orderId,
        UUID productId,
        Integer quantity,
        Instant occurredAt
) implements DomainEvent {

}
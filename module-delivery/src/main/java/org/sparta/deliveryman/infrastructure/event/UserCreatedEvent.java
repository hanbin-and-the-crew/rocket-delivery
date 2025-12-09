package org.sparta.deliveryman.infrastructure.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * User 생성 이벤트 (DeliveryMan 모듈에서 수신용)
 *
 * 주의: User 엔티티를 직접 참조하지 않음
 * - User 모듈과의 결합도 최소화
 * - Kafka JSON 메시지만으로 역직렬화 가능
 */
public record UserCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        UserPayload payload
) {
    /**
     * Jackson 역직렬화를 위한 생성자
     */
    @JsonCreator
    public UserCreatedEvent(
            @JsonProperty("eventId") UUID eventId,
            @JsonProperty("occurredAt") Instant occurredAt,
            @JsonProperty("payload") UserPayload payload
    ) {
        this.eventId = eventId;
        this.occurredAt = occurredAt;
        this.payload = payload;
    }
}
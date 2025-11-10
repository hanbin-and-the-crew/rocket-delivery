package org.sparta.order.infrastructure.event.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Product 서비스가 발행한 재고 관련 이벤트 Payload
 * - 소비자는 StockEventListener (KafkaListener) 가 이 DTO를 역직렬화하여 사용
 * - 호환성 강화를 위해 알 수 없는 필드는 무시(@JsonIgnoreProperties), 수량/사유 필드는 @JsonAlias 로 유연 매핑
 */
public final class StockEventDto {

    /** 재고 예약 성공 (stock-reserved) */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record StockReserved(
            UUID eventId,
            UUID orderId,
            UUID productId,
            @JsonAlias({"reservedQuantity", "quantity"}) Integer reservedQuantity,
            @JsonAlias({"occurredAt"}) String occurredAt,
            // 선택 필드: traceId 등 부가 메타를 보낼 수도 있으므로 alias 없이 열어둠
            String traceId
    ) {
        public OffsetDateTime occurredAtAsTime() {
            return occurredAt != null ? OffsetDateTime.parse(occurredAt) : null;
        }
    }

    /** 재고 예약 실패 (stock-reservation-failed) */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record StockReservationFailed(
            UUID eventId,
            UUID orderId,
            UUID productId,
            @JsonAlias({"requestedQuantity", "quantity"}) Integer requestedQuantity,
            @JsonAlias({"reason", "reasonCode"}) String reasonCode,
            @JsonAlias({"message", "errorMessage"}) String message,
            @JsonAlias({"occurredAt"}) String occurredAt,
            String traceId
    ) {
        public OffsetDateTime occurredAtAsTime() {
            return occurredAt != null ? OffsetDateTime.parse(occurredAt) : null;
        }
    }

    /** 재고 확정(결제 완료 등으로 확정) (stock-confirmed) */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record StockConfirmed(
            UUID eventId,
            UUID orderId,
            UUID productId,
            @JsonAlias({"confirmedQuantity", "quantity"}) Integer confirmedQuantity,
            @JsonAlias({"occurredAt"}) String occurredAt,
            String traceId
    ) {
        public OffsetDateTime occurredAtAsTime() {
            return occurredAt != null ? OffsetDateTime.parse(occurredAt) : null;
        }
    }

    /** 재고 예약 취소 완료 (stock-reservation-cancelled) */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record StockReservationCancelled(
            UUID eventId,
            UUID orderId,
            UUID productId,
            @JsonAlias({"cancelledQuantity", "canceledQuantity", "quantity"}) Integer cancelledQuantity,
            @JsonAlias({"occurredAt"}) String occurredAt,
            String traceId
    ) {
        public OffsetDateTime occurredAtAsTime() {
            return occurredAt != null ? OffsetDateTime.parse(occurredAt) : null;
        }
    }

    private StockEventDto() { /* no-op */ }
}

package org.sparta.payment.application.dto;

import org.sparta.payment.domain.entity.PaymentOutbox;
import org.sparta.common.domain.OutboxStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentOutboxDetailResult(
        UUID paymentOutboxId,
        String aggregateType,
        UUID aggregateId,
        String eventType,
        String payload,
        OutboxStatus status,
        Integer retryCount,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static PaymentOutboxDetailResult from(PaymentOutbox outbox) {
        return new PaymentOutboxDetailResult(
                outbox.getPaymentOutboxId(),
                outbox.getAggregateType(),
                outbox.getAggregateId(),
                outbox.getEventType(),
                outbox.getPayload(),
                outbox.getStatus(),
                outbox.getRetryCount(),
                outbox.getPublishedAt(),
                outbox.getCreatedAt(),
                outbox.getUpdatedAt()
        );
    }
}

package org.sparta.delivery.infrastructure.event;


import org.sparta.common.event.DomainEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

// TODO : Orer쪽에서 필드 추가해야 됨
public record OrderApprovedEvent(
        UUID eventId,
        UUID orderId,
        UUID customerId,
        UUID supplierCompanyId,
        UUID supplierHubId,
        UUID receiveCompanyId,
        UUID receiveHubId,
        String address,
        String receiverName,
        String receiverSlackId,
        String receiverPhone,
        LocalDateTime dueAt,
        String requestMemo,
        Instant occurredAt
) implements DomainEvent {
    public static OrderApprovedEvent of(UUID eventId,
                                        UUID orderId,
                                        UUID customerId,
                                        UUID supplierCompanyId,
                                        UUID supplierHubId,
                                        UUID receiveCompanyId,
                                        UUID receiveHubId,
                                        String address,
                                        String receiverName,
                                        String receiverSlackId,
                                        String receiverPhone,
                                        LocalDateTime dueAt,
                                        String requestMemo,
                                        Instant occurredAt) {
        return new OrderApprovedEvent(
                UUID.randomUUID(),
                orderId,
                customerId,
                supplierCompanyId,
                supplierHubId,
                receiveCompanyId,
                receiveHubId,
                address,
                receiverName,
                receiverSlackId,
                receiverPhone,
                dueAt,
                requestMemo,
                Instant.now()
        );
    }
}

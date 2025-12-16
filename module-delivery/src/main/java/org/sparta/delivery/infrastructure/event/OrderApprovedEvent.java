package org.sparta.delivery.infrastructure.event;


import org.sparta.common.event.DomainEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderApprovedEvent(
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
        UUID eventId,
        Instant occurredAt
) implements DomainEvent {
    public static OrderApprovedEvent of(
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
                                        String requestMemo
                                        ) {
        return new OrderApprovedEvent(
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
                UUID.randomUUID(),
                Instant.now()
        );
    }
}

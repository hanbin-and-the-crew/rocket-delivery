package org.sparta.order.infrastructure.event.publisher;

import org.sparta.common.event.DomainEvent;
import org.sparta.order.domain.entity.Order;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderApprovedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        UUID customerId,
        UUID receiveHubId,
        UUID receiveCompanyId,
        UUID supplierHubId,
        UUID supplierCompanyId,
        String address,
        String receiverPhone,
        String dueAt,
        String requestedMemo,
        String receiverSlackId
) implements DomainEvent {
    public static OrderApprovedEvent of(Order order) {
        return new OrderApprovedEvent(
                UUID.randomUUID(),              // eventId (멱등성 보장용)
                Instant.now(),
                order.getId(),
                order.getCustomerId(),
                order.getReceiveHubId(),
                order.getReceiveCompanyId(),
                order.getSupplierHubId(),
                order.getSupplierCompanyId(),
                order.getAddress(),
                order.getUserPhoneNumber(),
                order.getDueAt().toString(),
                order.getRequestMemo(),
                order.getSlackId()
        );
    }
}

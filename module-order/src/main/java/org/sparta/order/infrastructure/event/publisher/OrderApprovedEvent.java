package org.sparta.order.infrastructure.event.publisher;

import org.sparta.common.event.DomainEvent;
import org.sparta.order.domain.entity.Order;
import org.springframework.cglib.core.Local;

import java.time.Instant;
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
    public static OrderApprovedEvent of(Order order) {
        return new OrderApprovedEvent(
                order.getId(),
                order.getCustomerId(),
                order.getSupplierCompanyId(),
                order.getSupplierHubId(),
                order.getReceiveCompanyId(),
                order.getReceiveHubId(),
                order.getAddress(),
                order.getUserName(),
                order.getSlackId(),
                order.getUserPhoneNumber(),
                order.getDueAt().getTime(),
                order.getRequestMemo(),
                UUID.randomUUID(),              // eventId (멱등성 보장용)
                Instant.now()
        );
    }
}

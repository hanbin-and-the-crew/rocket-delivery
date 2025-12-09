package org.sparta.user.infrastructure.event;

import java.time.Instant;
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
) {
}
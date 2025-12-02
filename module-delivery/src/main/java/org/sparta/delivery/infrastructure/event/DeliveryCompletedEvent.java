package org.sparta.delivery.infrastructure.event;

import org.sparta.common.event.DomainEvent;
import org.sparta.delivery.domain.entity.Delivery;
import org.sparta.delivery.domain.enumeration.DeliveryStatus;
import org.sparta.deliverylog.domain.entity.DeliveryLog;
import org.sparta.deliverylog.domain.enumeration.DeliveryLogStatus;

import java.time.Instant;
import java.util.UUID;

public record DeliveryCompletedEvent(
        UUID eventId,
        UUID deliveryId,
        UUID orderId,
        int sequence,
        UUID sourceHubId,
        UUID targetHubId,
        double actualKm,
        int actualMinutes,
        boolean lastLeg,                 // 마지막 허브 leg 여부
        DeliveryStatus deliveryStatus,   // DEST_HUB_ARRIVED or HUB_WAITING
        DeliveryLogStatus logStatus,     // HUB_ARRIVED
        Instant occurredAt
) implements DomainEvent {

    public static DeliveryCompletedEvent from(
            Delivery delivery,
            DeliveryLog log,
            boolean lastLeg
    ) {
        return new DeliveryCompletedEvent(
                UUID.randomUUID(),
                delivery.getId(),
                delivery.getOrderId(),
                log.getSequence(),
                log.getSourceHubId(),
                log.getTargetHubId(),
                log.getActualKm(),
                log.getActualMinutes(),
                lastLeg,
                delivery.getStatus(),         // 마지막이면 DEST_HUB_ARRIVED
                log.getStatus(),              // HUB_ARRIVED
                Instant.now()
        );
    }
}

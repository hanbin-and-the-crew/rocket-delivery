package org.sparta.deliverylog.application.dto;

import org.sparta.deliverylog.domain.entity.DeliveryLog;

import java.time.LocalDateTime;
import java.util.UUID;

public class DeliveryLogResponse {

    public record Summary(
            UUID deliveryLogId,
            UUID deliveryId,
            Integer hubSequence,
            UUID departureHubId,
            UUID destinationHubId,
            UUID deliveryManId,
            String status,
            LocalDateTime createdAt
    ) {
        public Summary(DeliveryLog deliveryLog) {
            this(
                    deliveryLog.getDeliveryLogId(),
                    deliveryLog.getDeliveryId(),
                    deliveryLog.getHubSequence(),
                    deliveryLog.getDepartureHubId(),
                    deliveryLog.getDestinationHubId(),
                    deliveryLog.getDeliveryManId(),
                    deliveryLog.getDeliveryStatus().name(),
                    deliveryLog.getCreatedAt()
            );
        }
    }

    public record Detail(
            UUID deliveryLogId,
            UUID deliveryId,
            Integer hubSequence,
            UUID departureHubId,
            UUID destinationHubId,
            UUID deliveryManId,
            Double expectedDistance,
            Integer expectedTime,
            Double actualDistance,
            Integer actualTime,
            String status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
        public Detail(DeliveryLog deliveryLog) {
            this(
                    deliveryLog.getDeliveryLogId(),
                    deliveryLog.getDeliveryId(),
                    deliveryLog.getHubSequence(),
                    deliveryLog.getDepartureHubId(),
                    deliveryLog.getDestinationHubId(),
                    deliveryLog.getDeliveryManId(),
                    deliveryLog.getExpectedDistance() != null ?
                            deliveryLog.getExpectedDistance().getValue() : null,
                    deliveryLog.getExpectedTime() != null ?
                            deliveryLog.getExpectedTime().getValue() : null,
                    deliveryLog.getActualDistance() != null ?
                            deliveryLog.getActualDistance().getValue() : null,
                    deliveryLog.getActualTime() != null ?
                            deliveryLog.getActualTime().getValue() : null,
                    deliveryLog.getDeliveryStatus().name(),
                    deliveryLog.getCreatedAt(),
                    deliveryLog.getUpdatedAt(),
                    deliveryLog.getDeletedAt()
            );
        }
    }
}

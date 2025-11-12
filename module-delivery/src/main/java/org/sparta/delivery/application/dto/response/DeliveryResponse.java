package org.sparta.delivery.application.dto.response;

import org.sparta.delivery.domain.entity.Delivery;
import org.sparta.delivery.domain.enumeration.DeliveryStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public sealed interface DeliveryResponse permits
        DeliveryResponse.Create,
        DeliveryResponse.Detail,
        DeliveryResponse.Summary {

    record Create(
            UUID deliveryId,
            UUID orderId,
            DeliveryStatus deliveryStatus,
            UUID departureHubId,
            UUID destinationHubId,
            String deliveryAddress,
            String recipientName,
            LocalDateTime createdAt
    ) implements DeliveryResponse {
        public static Create of(Delivery delivery) {
            return new Create(
                    delivery.getId(),
                    delivery.getOrderId(),
                    delivery.getDeliveryStatus(),
                    delivery.getDepartureHubId(),
                    delivery.getDestinationHubId(),
                    delivery.getDeliveryAddress(),
                    delivery.getRecipientName(),
                    delivery.getCreatedAt()
            );
        }
    }

    record Detail(
            UUID deliveryId,
            UUID orderId,
            DeliveryStatus deliveryStatus,
            UUID departureHubId,
            UUID destinationHubId,
            String deliveryAddress,
            String recipientName,
            String recipientSlackId,
            UUID companyDeliveryManId,
            UUID hubDeliveryManId,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) implements DeliveryResponse {
        public static Detail of(Delivery delivery) {
            return new Detail(
                    delivery.getId(),
                    delivery.getOrderId(),
                    delivery.getDeliveryStatus(),
                    delivery.getDepartureHubId(),
                    delivery.getDestinationHubId(),
                    delivery.getDeliveryAddress(),
                    delivery.getRecipientName(),
                    delivery.getRecipientSlackId(),
                    delivery.getCompanyDeliveryManId(),
                    delivery.getHubDeliveryManId(),
                    delivery.getCreatedAt(),
                    delivery.getUpdatedAt()
            );
        }
    }

    record Summary(
            UUID deliveryId,
            UUID orderId,
            DeliveryStatus deliveryStatus,
            String deliveryAddress,
            String recipientName
    ) implements DeliveryResponse {
        public static Summary of(Delivery delivery) {
            return new Summary(
                    delivery.getId(),
                    delivery.getOrderId(),
                    delivery.getDeliveryStatus(),
                    delivery.getDeliveryAddress(),
                    delivery.getRecipientName()
            );
        }
    }
}

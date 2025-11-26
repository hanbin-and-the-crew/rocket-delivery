package org.sparta.deliveryman.application.dto;

import org.sparta.deliveryman.domain.enumeration.DeliveryManStatus;
import org.sparta.deliveryman.domain.enumeration.DeliveryManType;

import java.util.UUID;

public record DeliveryManSearchCondition(
        UUID userId,
        UUID affiliationHubId,
        DeliveryManType deliveryManagerType,
        DeliveryManStatus status,
        String userName,
        String email
) {
}
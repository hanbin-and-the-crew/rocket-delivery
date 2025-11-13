package org.sparta.deliveryman.application.dto;

import org.sparta.deliveryman.domain.entity.DeliveryMan;

import java.util.UUID;

public class DeliveryManResponse {

    public record Summary(
            UUID id,
            String userName,
            String email,
            String phoneNumber,
            UUID affiliationHubId,
            String status
    ) {
        public Summary(DeliveryMan dm) {
            this(dm.getId(), dm.getUserName(), dm.getEmail(), dm.getPhoneNumber(), dm.getAffiliationHubId(), dm.getStatus().name());
        }
    }

    public record Detail(
            UUID id,
            UUID userId,
            String userName,
            String email,
            String phoneNumber,
            UUID affiliationHubId,
            String slackId,
            String deliveryManType,
            String status,
            int assignedDeliveryCount,
            String lastDeliveryCompletedAt,
            int deliverySequence
    ) {
        public static Detail fromEntity(DeliveryMan deliveryMan) {
            return new Detail(
                    deliveryMan.getId(),
                    deliveryMan.getUserId(),
                    deliveryMan.getUserName(),
                    deliveryMan.getEmail(),
                    deliveryMan.getPhoneNumber(),
                    deliveryMan.getAffiliationHubId(),
                    deliveryMan.getSlackId(),
                    deliveryMan.getDeliveryManType().name(),
                    deliveryMan.getStatus().name(),
                    deliveryMan.getAssignedDeliveryCount(),
                    deliveryMan.getLastDeliveryCompletedAt() != null ? deliveryMan.getLastDeliveryCompletedAt().toString() : null,
                    deliveryMan.getDeliverySequence()
            );
        }
    }
}

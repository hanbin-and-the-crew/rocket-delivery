//package org.sparta.deliveryman.domain.event;
//
//import lombok.Getter;
//import lombok.ToString;
//import org.sparta.deliveryman.domain.entity.DeliveryMan;
//
//import java.util.UUID;
//
//@Getter
//@ToString
//public class DeliveryManUpdatedEvent {
//
//    private final UUID id;
//    private final String userName;
//    private final String email;
//    private final String phoneNumber;
//    private final UUID affiliationHubId;
//    private final String slackId;
//    private final String deliveryManType;
//    private final String status;
//
//    private DeliveryManUpdatedEvent(UUID id, String userName, String email, String phoneNumber,
//                                    UUID affiliationHubId, String slackId,
//                                    String deliveryManType, String status) {
//        this.id = id;
//        this.userName = userName;
//        this.email = email;
//        this.phoneNumber = phoneNumber;
//        this.affiliationHubId = affiliationHubId;
//        this.slackId = slackId;
//        this.deliveryManType = deliveryManType;
//        this.status = status;
//    }
//
//    public static DeliveryManUpdatedEvent from(DeliveryMan deliveryMan) {
//        return new DeliveryManUpdatedEvent(
//                deliveryMan.getId(),
//                deliveryMan.getUserName(),
//                deliveryMan.getEmail(),
//                deliveryMan.getPhoneNumber(),
//                deliveryMan.getAffiliationHubId(),
//                deliveryMan.getSlackId(),
//                deliveryMan.getDeliveryManType().name(),
//                deliveryMan.getStatus().name()
//        );
//    }
//}

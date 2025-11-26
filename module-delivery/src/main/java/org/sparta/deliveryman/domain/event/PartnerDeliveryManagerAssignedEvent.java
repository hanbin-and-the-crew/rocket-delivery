package org.sparta.deliveryman.domain.event;

import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

@Getter
@ToString
public class PartnerDeliveryManagerAssignedEvent {
    private final UUID deliveryId;
    private final UUID partnerManagerId;
    private final String managerName;
    private final String managerPhone;
    // 필요 시 추가 필드

    public PartnerDeliveryManagerAssignedEvent(UUID deliveryId, UUID partnerManagerId, String managerName, String managerPhone) {
        this.deliveryId = deliveryId;
        this.partnerManagerId = partnerManagerId;
        this.managerName = managerName;
        this.managerPhone = managerPhone;
    }

    public static PartnerDeliveryManagerAssignedEvent of(UUID deliveryId, org.sparta.deliveryman.domain.entity.DeliveryMan manager) {
        return new PartnerDeliveryManagerAssignedEvent(
                deliveryId,
                manager.getId(),
                manager.getUserName(),
                manager.getPhoneNumber()
        );
    }
}

package org.sparta.deliveryman.domain.event;

import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

@Getter
@ToString
public class HubDeliveryManagerAssignedEvent {
    private final UUID deliveryId;
    private final UUID deliveryManagerId;
    private final String managerName;
    private final String managerPhone;
    // 필요 시 추가 필드

    public HubDeliveryManagerAssignedEvent(UUID deliveryId, UUID deliveryManagerId, String managerName, String managerPhone) {
        this.deliveryId = deliveryId;
        this.deliveryManagerId = deliveryManagerId;
        this.managerName = managerName;
        this.managerPhone = managerPhone;
    }

    public static HubDeliveryManagerAssignedEvent of(UUID deliveryId, org.sparta.deliveryman.domain.entity.DeliveryMan manager) {
        return new HubDeliveryManagerAssignedEvent(
                deliveryId,
                manager.getId(),
                manager.getUserName(),
                manager.getPhoneNumber()
        );
    }
}

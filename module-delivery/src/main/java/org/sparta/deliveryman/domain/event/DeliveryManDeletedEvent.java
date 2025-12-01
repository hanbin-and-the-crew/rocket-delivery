package org.sparta.deliveryapplication.deliveryman.domain.event;

import lombok.Getter;
import lombok.ToString;
import org.sparta.deliveryapplication.deliveryman.domain.entity.DeliveryMan;

import java.util.UUID;

@Getter
@ToString
public class DeliveryManDeletedEvent {

    private final UUID id;

    private DeliveryManDeletedEvent(UUID id) {
        this.id = id;
    }

    public static DeliveryManDeletedEvent from(DeliveryMan deliveryMan) {
        return new DeliveryManDeletedEvent(deliveryMan.getId());
    }
}

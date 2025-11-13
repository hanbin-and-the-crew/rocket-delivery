package org.sparta.deliveryman.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.deliveryman.domain.enumeration.DeliveryManStatus;
import org.sparta.deliveryman.domain.enumeration.DeliveryManType;
import org.sparta.deliveryman.domain.event.DeliveryManCreatedEvent;
import org.sparta.deliveryman.domain.event.DeliveryManDeletedEvent;
import org.sparta.deliveryman.domain.event.DeliveryManUpdatedEvent;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_delivery_managers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryMan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "user_name", nullable = false, length = 100)
    private String userName;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "affiliation_hub_id")
    private UUID affiliationHubId;

    @Column(name = "slack_id", length = 50)
    private String slackId;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_man_type", nullable = false, length = 30)
    private DeliveryManType deliveryManType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DeliveryManStatus status;

    @Column(name = "assigned_delivery_count", nullable = false)
    private int assignedDeliveryCount;

    @Column(name = "last_delivery_completed_at")
    private LocalDateTime lastDeliveryCompletedAt;

    @Column(name = "delivery_sequence", nullable = false)
    private int deliverySequence;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static DeliveryMan create(UUID userId, String userName, String email, String phoneNumber,
                                     UUID affiliationHubId, String slackId,
                                     DeliveryManType deliveryManType, DeliveryManStatus status) {
        DeliveryMan dm = new DeliveryMan();
        dm.userId = userId;
        dm.userName = userName;
        dm.email = email;
        dm.phoneNumber = phoneNumber;
        dm.affiliationHubId = affiliationHubId;
        dm.slackId = slackId;
        dm.deliveryManType = deliveryManType;
        dm.status = status;
        dm.assignedDeliveryCount = 0;
        dm.deliverySequence = 0;
        dm.lastDeliveryCompletedAt = null;
        return dm;
    }

    public void update(String userName, String email, String phoneNumber,
                       UUID affiliationHubId, String slackId,
                       DeliveryManType deliveryManType, DeliveryManStatus status) {
        this.userName = userName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.affiliationHubId = affiliationHubId;
        this.slackId = slackId;
        this.deliveryManType = deliveryManType;
        this.status = status;
    }

    public void changeStatus(DeliveryManStatus newStatus) {
        this.status = newStatus;
    }

    public void incrementAssignedDeliveryCount() {
        this.assignedDeliveryCount++;
    }

    public void decrementAssignedDeliveryCount() {
        if (this.assignedDeliveryCount > 0) {
            this.assignedDeliveryCount--;
        }
    }

    public void updateLastDeliveryCompletedAt(LocalDateTime completedAt) {
        this.lastDeliveryCompletedAt = completedAt;
    }

    public void setDeliverySequence(int sequence) {
        this.deliverySequence = sequence;
    }

    public void delete() {
        this.status = DeliveryManStatus.INACTIVE;
        this.deletedAt = LocalDateTime.now();
    }

    public DeliveryManCreatedEvent toCreatedEvent() {
        return DeliveryManCreatedEvent.from(this);
    }

    public DeliveryManUpdatedEvent toUpdatedEvent() {
        return DeliveryManUpdatedEvent.from(this);
    }

    public DeliveryManDeletedEvent toDeletedEvent() {
        return DeliveryManDeletedEvent.from(this);
    }
}

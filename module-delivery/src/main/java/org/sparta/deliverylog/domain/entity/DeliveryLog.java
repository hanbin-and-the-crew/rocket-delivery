package org.sparta.deliverylog.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.deliverylog.domain.enumeration.DeliveryRouteStatus;
import org.sparta.deliverylog.domain.vo.Distance;
import org.sparta.deliverylog.domain.vo.Duration;
import org.sparta.jpa.entity.BaseEntity;

import java.util.UUID;

@Entity
@Getter
@Table(name = "p_delivery_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID deliveryLogId;

    @Column(nullable = false)
    private UUID deliveryId;

    @Column(nullable = false)
    private Integer hubSequence;

    @Column(nullable = false)
    private UUID departureHubId;

    @Column(nullable = false)
    private UUID destinationHubId;

    @Column(name = "delivery_man_id")
    private UUID deliveryManId;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "expected_distance"))
    private Distance expectedDistance;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "expected_time"))
    private Duration expectedTime;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "actual_distance"))
    private Distance actualDistance;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "actual_time"))
    private Duration actualTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false)
    private DeliveryRouteStatus deliveryStatus = DeliveryRouteStatus.WAITING;

    // ========== Static Factory Method ==========

    public static DeliveryLog create(
            UUID deliveryId,
            Integer hubSequence,
            UUID departureHubId,
            UUID destinationHubId,
            Double expectedDistance,
            Integer expectedTime
    ) {
        DeliveryLog deliveryLog = new DeliveryLog();
        deliveryLog.deliveryId = deliveryId;
        deliveryLog.hubSequence = hubSequence;
        deliveryLog.departureHubId = departureHubId;
        deliveryLog.destinationHubId = destinationHubId;
        deliveryLog.expectedDistance = Distance.of(expectedDistance);
        deliveryLog.expectedTime = Duration.of(expectedTime);
        deliveryLog.deliveryStatus = DeliveryRouteStatus.WAITING;
        return deliveryLog;
    }

    // ========== 비즈니스 로직 ==========

    public void assignDeliveryMan(UUID deliveryManId) {
        if (deliveryManId == null) {
            throw new IllegalArgumentException("배송 담당자 ID는 필수입니다");
        }
        this.deliveryManId = deliveryManId;
    }

    public void startDelivery() {
        if (this.deliveryStatus != DeliveryRouteStatus.WAITING) {
            throw new IllegalStateException("대기 중 상태에서만 배송을 시작할 수 있습니다");
        }
        if (this.deliveryManId == null) {
            throw new IllegalStateException("배송 담당자가 배정되지 않았습니다");
        }
        this.deliveryStatus = DeliveryRouteStatus.MOVING;
    }

    public void completeDelivery(Double actualDistance, Integer actualTime) {
        if (this.deliveryStatus != DeliveryRouteStatus.MOVING) {
            throw new IllegalStateException("이동 중 상태에서만 배송을 완료할 수 있습니다");
        }
        this.actualDistance = Distance.of(actualDistance);
        this.actualTime = Duration.of(actualTime);
        this.deliveryStatus = DeliveryRouteStatus.COMPLETED;
    }

    public void cancel() {
        if (this.deliveryStatus == DeliveryRouteStatus.COMPLETED) {
            throw new IllegalStateException("완료된 배송은 취소할 수 없습니다");
        }
        this.deliveryStatus = DeliveryRouteStatus.CANCELED;
    }

    public void updateExpectedValues(Double expectedDistance, Integer expectedTime) {
        if (this.deliveryStatus != DeliveryRouteStatus.WAITING) {
            throw new IllegalStateException("대기 중 상태에서만 예상 값을 수정할 수 있습니다");
        }
        if (expectedDistance != null) {
            this.expectedDistance = Distance.of(expectedDistance);
        }
        if (expectedTime != null) {
            this.expectedTime = Duration.of(expectedTime);
        }
    }

    public void changeDeliveryMan(UUID newDeliveryManId) {
        if (newDeliveryManId == null) {
            throw new IllegalArgumentException("새 배송 담당자 ID는 필수입니다");
        }
        if (this.deliveryStatus == DeliveryRouteStatus.COMPLETED) {
            throw new IllegalStateException("완료된 배송은 담당자를 변경할 수 없습니다");
        }
        this.deliveryManId = newDeliveryManId;
    }
}

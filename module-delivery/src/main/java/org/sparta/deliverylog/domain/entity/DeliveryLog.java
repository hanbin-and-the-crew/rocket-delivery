package org.sparta.deliverylog.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.common.error.BusinessException;
import org.sparta.deliverylog.domain.enumeration.DeliveryLogStatus;
import org.sparta.deliverylog.domain.error.DeliveryLogErrorType;
import org.sparta.jpa.entity.BaseEntity;

import java.util.UUID;


@Entity
@Table(name = "p_delivery_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID")
    private UUID id;

    // ===== 연관 =====

    @Column(name = "delivery_id", nullable = false, columnDefinition = "UUID")
    private UUID deliveryId;

    @Column(name = "sequence", nullable = false)
    private int sequence;

    @Column(name = "source_hub_id", nullable = false, columnDefinition = "UUID")
    private UUID sourceHubId;

    @Column(name = "target_hub_id", nullable = false, columnDefinition = "UUID")
    private UUID targetHubId;

    @Column(name = "estimated_km", nullable = false)
    private double estimatedKm;

    @Column(name = "estimated_minutes", nullable = false)
    private int estimatedMinutes;

    @Column(name = "actual_km")
    private Double actualKm;

    @Column(name = "actual_minutes")
    private Integer actualMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private DeliveryLogStatus status;

    @Column(name = "delivery_man_id", columnDefinition = "UUID")
    private UUID deliveryManId;

    // ===== 생성 메서드 =====

    public static DeliveryLog create(
            UUID deliveryId,
            int sequence,
            UUID sourceHubId,
            UUID targetHubId,
            double estimatedKm,
            int estimatedMinutes
    ) {
        //검증 -> 메소드로 따로 뺀 형태
        validateCreateArgs(deliveryId, sequence, sourceHubId, targetHubId,
                estimatedKm, estimatedMinutes);

        DeliveryLog log = new DeliveryLog();
        log.deliveryId = deliveryId;
        log.sequence = sequence;
        log.sourceHubId = sourceHubId;
        log.targetHubId = targetHubId;
        log.estimatedKm = estimatedKm;
        log.estimatedMinutes = estimatedMinutes;
        log.status = DeliveryLogStatus.CREATED;
        return log;
    }

    private static void validateCreateArgs(
            UUID deliveryId,
            int sequence,
            UUID sourceHubId,
            UUID targetHubId,
            double estimatedKm,
            int estimatedMinutes
    ) {
        if (deliveryId == null) {
            throw new BusinessException(DeliveryLogErrorType.DELIVERY_ID_REQUIRED);
        }
        if (sequence < 0) {
            throw new BusinessException(DeliveryLogErrorType.SEQUENCE_REQUIRED);
        }
        if (sourceHubId == null) {
            throw new BusinessException(DeliveryLogErrorType.SOURCE_HUB_ID_REQUIRED);
        }
        if (targetHubId == null) {
            throw new BusinessException(DeliveryLogErrorType.TARGET_HUB_ID_REQUIRED);
        }
        if (estimatedKm < 0) {
            throw new BusinessException(DeliveryLogErrorType.INVALID_ESTIMATED_KM);
        }
        if (estimatedMinutes < 0) {
            throw new BusinessException(DeliveryLogErrorType.INVALID_ESTIMATED_MINUTES);
        }
    }

    // ===== 담당자 배정 직후 status/CREATED -> HUB_WAITING 변경
     /**
     * 허브 배송 담당자 배정 완료 시 호출.
     * - CREATED 상태에서만 호출하는 것을 추천 (검증은 필요 시 추가).
     * - status 를 HUB_WAITING 으로 변경하고 deliveryManId 세팅.
     */
    public void assignDeliveryMan(UUID deliveryManId) {
        if (deliveryManId == null) {
            throw new BusinessException(DeliveryLogErrorType.DELIVERY_MAN_ID_REQUIRED);
        }
        if (this.status != DeliveryLogStatus.CREATED) {
            throw new BusinessException(DeliveryLogErrorType.INVALID_STATUS_TRANSITION);
        }
        this.deliveryManId = deliveryManId;
        this.status = DeliveryLogStatus.HUB_WAITING;
    }

    // 허브 출발 처리 / HUB_WAITING -> HUB_MOVING
    public void markMoving( ) {

        if (this.status != DeliveryLogStatus.HUB_WAITING) {
            // 허브 대기(HUB_WAITING) 상태에서만 출발 가능
            throw new BusinessException(DeliveryLogErrorType.INVALID_STATUS_TRANSITION);
        }

        this.status = DeliveryLogStatus.HUB_MOVING;
    }

    // ===== 허브 도착 처리 (해당 log 완료)/ HUB_MOVING -> HUB_ARRIVED
    // 실제 거리와 시간도 이 시점에서 갱신 (deliveryMan이 도착과 함께 같이 입력 가정)
    public void markArrived(double actualKm, int actualMinutes) {
        if (this.status != DeliveryLogStatus.HUB_MOVING) {
            // 실제 이동 중인 log만 완료 가능
            throw new BusinessException(DeliveryLogErrorType.INVALID_STATUS_TRANSITION);
        }
        if (actualKm <= 0) {
            throw new BusinessException(DeliveryLogErrorType.INVALID_ACTUAL_KM);
        }
        if (actualMinutes <= 0) {
            throw new BusinessException(DeliveryLogErrorType.INVALID_ACTUAL_MINUTES);
        }

        this.actualKm = actualKm;
        this.actualMinutes = actualMinutes;
        this.status = DeliveryLogStatus.HUB_ARRIVED;
    }

    /**
     * 배송/주문/로그 취소 시.
     * - 어떤 상태에서든 CANCELED 로 전환 가능하되, 이미 CANCELED면 무시.
     */
    public void cancelFromDelivery() {
        // 허용 상태: CREATED, HUB_WAITING
        if (this.status != DeliveryLogStatus.CREATED
                && this.status != DeliveryLogStatus.HUB_WAITING) {
            // 이미 HUB_MOVING 이후(이동중/도착/취소)는 취소 불가능
            throw new BusinessException(DeliveryLogErrorType.INVALID_STATUS_TRANSITION);
        }
        this.status = DeliveryLogStatus.CANCELED;
    }

    public void delete() {
        if (this.status == DeliveryLogStatus.CANCELED && this.getDeletedAt() != null) {
            return; // 이미 취소되거나 삭제된 경우 무시
        }
        this.status = DeliveryLogStatus.CANCELED;
        this.markAsDeleted(); // BaseEntity.deletedAt 세팅
    }
}
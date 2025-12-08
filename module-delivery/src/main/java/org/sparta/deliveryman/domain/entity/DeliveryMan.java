package org.sparta.deliveryman.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.common.error.BusinessException;
import org.sparta.deliveryman.domain.enumeration.DeliveryManStatus;
import org.sparta.deliveryman.domain.enumeration.DeliveryManType;
import org.sparta.deliveryman.domain.error.DeliveryManErrorType;
import org.sparta.jpa.entity.BaseEntity;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "p_delivery_men")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryMan extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "user_id", nullable = false, columnDefinition = "UUID")
    private UUID userId;

    @Column(name = "hub_id", columnDefinition = "UUID")
    private UUID hubId;   // HUB 타입: null, COMPANY 타입: not null

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = false)
    private DeliveryManType type;    // HUB / COMPANY

    @Column(name = "real_name", length = 100, nullable = false)
    private String realName;

    @Column(name = "slack_id", length = 100)
    private String slackId;

    @Column(name = "user_role", length = 50, nullable = false)
    private String userRole;     // UserRoleEnum 스냅샷 (문자열)

    @Column(name = "user_status", length = 30, nullable = false)
    private String userStatus;   // UserStatusEnum 스냅샷 (문자열)

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private DeliveryManStatus status;    // WAITING, DELIVERING, OFFLINE, DELETED

    @Enumerated(EnumType.STRING)
    @Column(name = "before_status", length = 30)
    private DeliveryManStatus beforeStatus;    // WAITING, DELIVERING, OFFLINE, DELETED (기본 상태는 WAITING)

    @Column(name = "sequence", nullable = false)
    private int sequence;        // 라운드 로빈 순번

    @Column(name = "delivery_count", nullable = false)
    private int deliveryCount;   // 누적 담당 배송 수 (tie-breaker 용)


    // ========= 정적 생성 메서드 =========

    /**
     * 허브 배송 담당자 생성 (전체 허브 공용)
     * - hubId = null
     * - type = HUB
     */
    public static DeliveryMan createHubDeliveryMan(
            UUID userId,
            String realName,
            String slackId,
            String userRole,
            String userStatus,
            int sequence
    ) {
        validateCommonCreateArgs(userId, realName, userRole, userStatus, sequence);

        DeliveryMan dm = new DeliveryMan();
        dm.userId = userId;
        dm.hubId = null;
        dm.type = DeliveryManType.HUB;
        dm.realName = realName;
        dm.slackId = slackId;
        dm.userRole = userRole;
        dm.userStatus = userStatus;
        dm.status = DeliveryManStatus.WAITING; // 기본은 대기 상태
        dm.beforeStatus =  DeliveryManStatus.WAITING; // 기본은 대기 상태로 동일
        dm.sequence = sequence;
        dm.deliveryCount = 0;
        return dm;
    }

    /**
     * 업체 배송 담당자 생성 (허브별 10명)
     * - hubId != null
     * - type = COMPANY
     */
    public static DeliveryMan createCompanyDeliveryMan(
            UUID userId,
            UUID hubId,
            String realName,
            String slackId,
            String userRole,
            String userStatus,
            int sequence
    ) {
        validateCommonCreateArgs(userId, realName, userRole, userStatus, sequence);
        if (hubId == null) {
            throw new BusinessException(DeliveryManErrorType.HUB_ID_REQUIRED_FOR_COMPANY);
        }

        DeliveryMan dm = new DeliveryMan();
        dm.userId = userId;
        dm.hubId = hubId;
        dm.type = DeliveryManType.COMPANY;
        dm.realName = realName;
        dm.slackId = slackId;
        dm.userRole = userRole;
        dm.userStatus = userStatus;
        dm.status = DeliveryManStatus.WAITING;
        dm.beforeStatus =  DeliveryManStatus.WAITING; // 기본은 대기 상태로 동일
        dm.sequence = sequence;
        dm.deliveryCount = 0;
        return dm;
    }

    private static void validateCommonCreateArgs(
            UUID userId,
            String realName,
            String userRole,
            String userStatus,
            int sequence
    ) {
        if (userId == null) {
            throw new BusinessException(DeliveryManErrorType.USER_ID_REQUIRED);
        }
        if (realName == null || realName.isBlank()) {
            throw new BusinessException(DeliveryManErrorType.REAL_NAME_REQUIRED);
        }
        if (userRole == null || userRole.isBlank()) {
            throw new BusinessException(DeliveryManErrorType.USER_ROLE_REQUIRED);
        }
        if (userStatus == null || userStatus.isBlank()) {
            throw new BusinessException(DeliveryManErrorType.USER_STATUS_REQUIRED);
        }
        if (sequence < 1) {
            throw new BusinessException(DeliveryManErrorType.SEQUENCE_REQUIRED);
        }
    }

    // ========= UserEvent 기반 정보 수정 =========

    /**
     * UserUpdateEvent 수신 시 정보 동기화
     * - realName, slackId, hubId(Company 타입), userRole, userStatus 변경 가능
     * - 변경사항이 하나도 없으면 예외 (의미 없는 업데이트 방지)
     */
    public void updateFromUserEvent(
            String newRealName,
            String newSlackId,
            String newUserRole,
            String newUserStatus,
            UUID newHubId    // COMPANY 타입일 때만 의미 있음
    ) {
        if (this.status == DeliveryManStatus.DELETED) {
            throw new BusinessException(DeliveryManErrorType.ALREADY_SOFT_DELETED);
        }

        boolean changed = false;

        if (newRealName != null && !newRealName.isBlank()
                && !Objects.equals(this.realName, newRealName)) {
            this.realName = newRealName;
            changed = true;
        }

        if (!Objects.equals(this.slackId, newSlackId)) {
            this.slackId = newSlackId;
            changed = true;
        }

        if (newUserRole != null && !newUserRole.isBlank()
                && !Objects.equals(this.userRole, newUserRole)) {
            this.userRole = newUserRole;
            changed = true;
        }

        if (newUserStatus != null && !newUserStatus.isBlank()
                && !Objects.equals(this.userStatus, newUserStatus)) {
            this.userStatus = newUserStatus;
            changed = true;
        }

        // COMPANY 타입인 경우에만 hubId 변경 의미 있음
        if (this.type == DeliveryManType.COMPANY) {
            if (!Objects.equals(this.hubId, newHubId)) {
                if (newHubId == null) {
                    throw new BusinessException(DeliveryManErrorType.HUB_ID_REQUIRED_FOR_COMPANY);
                }
                this.hubId = newHubId;
                changed = true;
            }
        }

        if (!changed) {
            throw new BusinessException(DeliveryManErrorType.NO_CHANGES_TO_UPDATE);
        }
    }

    // ========= Status 변경 메서드 =========

    /**
     * 상태 변경 (컨트롤러/비즈니스 로직에서 직접 status 변경하는 경우)
     *
     * 허용 전이:
     *  - WAITING -> DELIVERING, OFFLINE
     *  - DELIVERING -> WAITING
     *  - OFFLINE -> WAITING
     *  - DELETED -> (전이 불가)
     */
    public void changeDeliveryManStatus(DeliveryManStatus newStatus) {
        if (newStatus == null) {
            throw new BusinessException(DeliveryManErrorType.STATUS_REQUIRED);
        }
        if (this.status == DeliveryManStatus.DELETED) {
            throw new BusinessException(DeliveryManErrorType.CANNOT_CHANGE_STATUS_DELETED);
        }
        if (this.status == newStatus) {
            throw new BusinessException(DeliveryManErrorType.STATUS_ALREADY_SAME);
        }

        switch (this.status) {
            case WAITING -> {
                if (newStatus != DeliveryManStatus.DELIVERING
                        && newStatus != DeliveryManStatus.OFFLINE) {
                    throw new BusinessException(DeliveryManErrorType.INVALID_STATUS_TRANSITION);
                }
            }
            case DELIVERING -> {
                if (newStatus != DeliveryManStatus.WAITING) {
                    throw new BusinessException(DeliveryManErrorType.INVALID_STATUS_TRANSITION);
                }
            }
            case OFFLINE -> {
                if (newStatus != DeliveryManStatus.WAITING) {
                    throw new BusinessException(DeliveryManErrorType.INVALID_STATUS_TRANSITION);
                }
            }
            default -> throw new BusinessException(DeliveryManErrorType.INVALID_STATUS_TRANSITION);
        }

        this.status = newStatus;
    }

    // ========= 배정 관련 메서드 =========

    /**
     * 신규 배송 1건 배송 담당자 배정
     * - WAITING이면 DELIVERING으로
     * - DELIVERING이면 유지
     * - OFFLINE/DELETED면 배정 불가
     * - deliveryCount는 항상 +1
     */
    public void assignForNewDelivery() {
        if (this.status == DeliveryManStatus.OFFLINE || this.status == DeliveryManStatus.DELETED) {
            throw new BusinessException(DeliveryManErrorType.CANNOT_ASSIGN_OFFLINE_OR_DELETED);
        }

        // 배정 전 상태 스냅샷
        this.beforeStatus = this.status;

        if (this.status == DeliveryManStatus.WAITING) {
            this.status = DeliveryManStatus.DELIVERING;
        }
        this.deliveryCount++;
    }

    // 배송 취소 시 롤백
    public void rollbackAssignedDelivery() {
        if (this.beforeStatus == DeliveryManStatus.WAITING && this.status == DeliveryManStatus.DELIVERING) {
            this.status = DeliveryManStatus.WAITING;
        }
        this.deliveryCount = Math.max(this.deliveryCount - 1, 0);
    }

    /**
     * UserDeletedEvent 수신 시 Soft Delete 처리
     */
    public void markDeletedFromUserDeletedEvent() {
        if (this.status == DeliveryManStatus.DELETED && this.getDeletedAt() != null) {
            return; // 이미 삭제된 경우 무시
        }
        this.status = DeliveryManStatus.DELETED;
        this.markAsDeleted(); // BaseEntity.deletedAt 세팅
    }
}

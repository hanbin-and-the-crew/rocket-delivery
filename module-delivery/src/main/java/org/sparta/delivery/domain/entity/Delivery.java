package org.sparta.delivery.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.common.error.BusinessException;
import org.sparta.delivery.domain.enumeration.DeliveryStatus;
import org.sparta.delivery.domain.error.DeliveryErrorType;
import org.sparta.jpa.entity.BaseEntity;


import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_deliveries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Delivery extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID")
    private UUID id;

    // ===== 주문/고객/업체/허브 스냅샷 =====

    @Column(name = "order_id", nullable = false, columnDefinition = "UUID")
    private UUID orderId;

    @Column(name = "customer_id", nullable = false, columnDefinition = "UUID")
    private UUID customerId;

    @Column(name = "supplier_company_id", nullable = false, columnDefinition = "UUID")
    private UUID supplierCompanyId;

    @Column(name = "supplier_hub_id", nullable = false, columnDefinition = "UUID")
    private UUID supplierHubId;

    @Column(name = "receive_company_id", nullable = false, columnDefinition = "UUID")
    private UUID receiveCompanyId;

    @Column(name = "receive_hub_id", nullable = false, columnDefinition = "UUID")
    private UUID receiveHubId;

    @Column(name = "address", length = 255, nullable = false)
    private String address;

    @Column(name = "receiver_name", length = 100, nullable = false)
    private String receiverName;

    @Column(name = "receiver_slack_id", length = 100)
    private String receiverSlackId;

    @Column(name = "receiver_phone", length = 30, nullable = false)
    private String receiverPhone;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(name = "requested_memo", length = 500)
    private String requestedMemo;

    // ===== 진행 상태 =====

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private DeliveryStatus status;

    @Column(name = "current_log_seq")
    private Integer currentLogSeq; // 현재 진행 중이거나 마지막으로 도착한 허브 leg 시퀀스 (없으면 null)

    @Column(name = "total_log_seq")
    private Integer totalLogSeq;   // 전체 허브 leg 개수 (0~N-1 시퀀스 기준 N)

    @Column(name = "hub_delivery_man_id", columnDefinition = "UUID")
    private UUID hubDeliveryManId;

    @Column(name = "company_delivery_man_id", columnDefinition = "UUID")
    private UUID companyDeliveryManId;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    // ===== 생성 메서드 =====

    public static Delivery createFromOrderApproved(
            UUID orderId,
            UUID customerId,
            UUID supplierCompanyId,
            UUID supplierHubId,
            UUID receiveCompanyId,
            UUID receiveHubId,
            String address,
            String receiverName,
            String receiverSlackId,
            String receiverPhone,
            LocalDateTime dueAt,
            String requestedMemo,
            Integer totalLogSeq // 허브 leg 전체 개수 (알고 있다면 세팅, 모르면 null)
    ) {
        validateCreateArgs(orderId, customerId, supplierCompanyId, supplierHubId,
                receiveCompanyId, receiveHubId, address, receiverName, receiverPhone);

        Delivery d = new Delivery();
        d.orderId = orderId;
        d.customerId = customerId;
        d.supplierCompanyId = supplierCompanyId;
        d.supplierHubId = supplierHubId;
        d.receiveCompanyId = receiveCompanyId;
        d.receiveHubId = receiveHubId;
        d.address = address;
        d.receiverName = receiverName;
        d.receiverSlackId = receiverSlackId;
        d.receiverPhone = receiverPhone;
        d.dueAt = dueAt;
        d.requestedMemo = requestedMemo;
        d.status = DeliveryStatus.CREATED;
        d.currentLogSeq = null;
        d.totalLogSeq = totalLogSeq;
        d.hubDeliveryManId = null;
        d.companyDeliveryManId = null;
        return d;
    }

    private static void validateCreateArgs(
            UUID orderId,
            UUID customerId,
            UUID supplierCompanyId,
            UUID supplierHubId,
            UUID receiveCompanyId,
            UUID receiveHubId,
            String address,
            String receiverName,
            String receiverPhone
    ) {
        if (orderId == null) {
            throw new BusinessException(DeliveryErrorType.ORDER_ID_REQUIRED);
        }
        if (customerId == null) {
            throw new BusinessException(DeliveryErrorType.CUSTOMER_ID_REQUIRED);
        }
        if (supplierCompanyId == null) {
            throw new BusinessException(DeliveryErrorType.SUPPLIER_COMPANY_ID_REQUIRED);
        }
        if (supplierHubId == null) {
            throw new BusinessException(DeliveryErrorType.SUPPLIER_HUB_ID_REQUIRED);
        }
        if (receiveCompanyId == null) {
            throw new BusinessException(DeliveryErrorType.RECEIVE_COMPANY_ID_REQUIRED);
        }
        if (receiveHubId == null) {
            throw new BusinessException(DeliveryErrorType.RECEIVE_HUB_ID_REQUIRED);
        }
        if (address == null || address.isBlank()) {
            throw new BusinessException(DeliveryErrorType.ADDRESS_REQUIRED);
        }
        if (receiverName == null || receiverName.isBlank()) {
            throw new BusinessException(DeliveryErrorType.RECEIVER_NAME_REQUIRED);
        }
        if (receiverPhone == null || receiverPhone.isBlank()) {
            throw new BusinessException(DeliveryErrorType.RECEIVER_PHONE_REQUIRED);
        }
    }

    // 필요 시 totalLogSeq를 나중에 세팅/수정하는 도메인 메서드
    public void updateTotalLogSeq(int totalLogSeq) {
        if (totalLogSeq < 0) {
            throw new BusinessException(DeliveryErrorType.INVALID_TOTAL_LOG_SEQ);
        }
        this.totalLogSeq = totalLogSeq;
    }

    // ===== 담당자 배정 =====

    public void assignHubDeliveryMan(UUID deliveryManId) {
        if (deliveryManId == null) {
            throw new BusinessException(DeliveryErrorType.DELIVERY_MAN_ID_REQUIRED);
        }
        if (status != DeliveryStatus.CREATED && status != DeliveryStatus.HUB_WAITING) {
            throw new BusinessException(DeliveryErrorType.INVALID_STATUS_FOR_HUB_ASSIGN);
        }
        this.hubDeliveryManId = deliveryManId;
        if (this.status == DeliveryStatus.CREATED) {
            this.status = DeliveryStatus.HUB_WAITING;
        }
    }

    public void assignCompanyDeliveryMan(UUID deliveryManId) {
        if (deliveryManId == null) {
            throw new BusinessException(DeliveryErrorType.DELIVERY_MAN_ID_REQUIRED);
        }
        if (status != DeliveryStatus.DEST_HUB_ARRIVED && status != DeliveryStatus.COMPANY_MOVING) {
            throw new BusinessException(DeliveryErrorType.INVALID_STATUS_FOR_COMPANY_ASSIGN);
        }
        this.companyDeliveryManId = deliveryManId;
    }

    // ===== 허브 leg 진행 =====

    public void startHubMoving(int seq) {
        if (this.status != DeliveryStatus.HUB_WAITING) {
            throw new BusinessException(DeliveryErrorType.INVALID_STATUS_FOR_HUB_START);
        }
        if (seq < 0) {
            throw new BusinessException(DeliveryErrorType.INVALID_LOG_SEQUENCE);
        }
        this.status = DeliveryStatus.HUB_MOVING;
        this.currentLogSeq = seq;
    }

    public void completeHubMoving(int seq, boolean isLastLog) {
        if (this.status != DeliveryStatus.HUB_MOVING) {
            throw new BusinessException(DeliveryErrorType.INVALID_STATUS_FOR_HUB_COMPLETE);
        }
        if (this.currentLogSeq == null || this.currentLogSeq != seq) {
            throw new BusinessException(DeliveryErrorType.INVALID_LOG_SEQUENCE);
        }

        if (isLastLog) {
            this.status = DeliveryStatus.DEST_HUB_ARRIVED;
            this.currentLogSeq = null;
        } else {
            this.status = DeliveryStatus.HUB_WAITING;
            this.currentLogSeq = seq;
        }
    }

    // ===== 업체 구간 진행 =====

    public void startCompanyMoving() {
        if (this.status != DeliveryStatus.DEST_HUB_ARRIVED) {
            throw new BusinessException(DeliveryErrorType.INVALID_STATUS_FOR_COMPANY_START);
        }
        this.status = DeliveryStatus.COMPANY_MOVING;
    }

    public void completeDelivery() {
        if (this.status != DeliveryStatus.COMPANY_MOVING) {
            throw new BusinessException(DeliveryErrorType.INVALID_STATUS_FOR_COMPLETE);
        }
        this.status = DeliveryStatus.DELIVERED;
    }

    // ===== 취소/삭제 =====

    public void cancel() {
        if (this.status == DeliveryStatus.CANCELED) {
            return;
        }
        this.status = DeliveryStatus.CANCELED;
        this.canceledAt = LocalDateTime.now(); // 추가
    }

    public void delete() {
        if (this.status == DeliveryStatus.CANCELED && this.getDeletedAt() != null) {
            return;
        }
        this.status = DeliveryStatus.CANCELED;
        this.markAsDeleted();
    }
}

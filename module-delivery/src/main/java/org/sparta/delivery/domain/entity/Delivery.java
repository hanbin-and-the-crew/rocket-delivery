package org.sparta.delivery.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.common.error.BusinessException;
import org.sparta.delivery.domain.enumeration.DeliveryStatus;
import org.sparta.delivery.domain.error.DeliveryErrorType;
import org.sparta.deliverylog.domain.entity.DeliveryLog;
import org.sparta.deliverylog.domain.enumeration.DeliveryLogStatus;
import org.sparta.jpa.entity.BaseEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

    // ===== 스냅샷 / 배송 정보 =====

    @Column(name = "address", length = 300, nullable = false)
    private String address;

    @Column(name = "receiver_name", length = 100, nullable = false)
    private String receiverName;

    @Column(name = "receiver_slack_id", length = 100)
    private String receiverSlackId;

    @Column(name = "receiver_phone", length = 50, nullable = false)
    private String receiverPhone;

    @Column(name = "due_at", nullable = false)
    private LocalDateTime dueAt;

    @Column(name = "requested_memo", length = 300)
    private String requestedMemo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private DeliveryStatus status;

    @Column(name = "current_log_seq")
    private Integer currentLogSeq;

    // 허브 전체 구간 담당자 (허브 배송 담당자 10명 중 한 명)
    @Column(name = "hub_delivery_Man_id")
    private UUID hubDeliveryManId;

    // 목적지 허브 → 업체 구간 담당자
    @Column(name = "company_delivery_Man_id")
    private UUID companyDeliveryManId;

    // ===== 연관 관계 =====

    @OneToMany(
            mappedBy = "delivery",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<DeliveryLog> logs = new ArrayList<>();

    // ===== 생성 메서드 =====
    // 주문 확정 -> 배송/로그 생성
    public static Delivery create(
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
            String requestedMemo
    ) {
        // null 검증
        if (orderId == null) {
            throw new BusinessException(DeliveryErrorType.ORDER_ID_REQUIRED);
        }
        if (customerId == null) {
            throw new BusinessException(DeliveryErrorType.CUSTOMER_ID_REQUIRED);
        }
        if (supplierCompanyId == null || supplierHubId == null) {
            throw new BusinessException(DeliveryErrorType.SUPPLIER_INFO_REQUIRED);
        }
        if (receiveCompanyId == null || receiveHubId == null) {
            throw new BusinessException(DeliveryErrorType.RECEIVER_INFO_REQUIRED);
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
        if (dueAt == null) {
            throw new BusinessException(DeliveryErrorType.DUE_AT_REQUIRED);
        }

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
        d.hubDeliveryManId = null;
        d.companyDeliveryManId = null;

        return d;
    }

    // 양방향 연관관계 편의 메서드
    public void addLog(DeliveryLog log) {
        if (log == null) {
            throw new IllegalArgumentException("배송 로그는 빈값일 수 없습니다.");
        }
        logs.add(log);
        //TODO :해결 필요
//        log.setDelivery(this);
    }

    // ====== 비즈니스 메서드 ======

    // 시퀀스로 log 찾기
    private DeliveryLog getLogBySequenceOrThrow(int sequence) {
        return logs.stream()
                .filter(l -> l.getSequence() == sequence)
                .findFirst()
                .orElseThrow(() ->
                        new BusinessException(DeliveryErrorType.LOG_NOT_FOUND_FOR_SEQUENCE));
    }

    // Hub 담당자 배정 완료 시 -> HUB_WAITING 변경
    // DeliveryCreatedEvent 수신 후 호출
    public void markHubWaitingAfterAssignment() {
        if (status != DeliveryStatus.CREATED) {
            throw new BusinessException(DeliveryErrorType.INVALID_STATUS_FOR_ASSIGNMENT);
        }

        this.status = DeliveryStatus.HUB_WAITING;

        // 생성된 모든 허브 leg를 HUB_WAITING 으로 전환
        for (DeliveryLog log : logs) {
            if (log.getStatus() != DeliveryLogStatus.CREATED) {
                throw new BusinessException(DeliveryErrorType.INVALID_LOG_STATUS_FOR_ASSIGNMENT);
            }
            log.markHubWaiting();    // CREATED -> HUB_WAITING
        }
    }

    // 허브 담당자 사전 배정 (DeliveryCreatedEvent 이후)
    public void assignHubDeliveryMan(UUID hubDeliveryManId) {
        if (hubDeliveryManId == null) {
            throw new BusinessException(DeliveryErrorType.DELIVERYMAN_ID_REQUIRED);
        }
        // 이미 배정된 상태에서 다른 사람으로 바꾸려 하면 정책에 따라 검증
        if (this.hubDeliveryManId != null && !this.hubDeliveryManId.equals(hubDeliveryManId)) {
            throw new BusinessException(DeliveryErrorType.HUB_DELIVERYMAN_MISMATCH);
        }
        this.hubDeliveryManId = hubDeliveryManId;
    }

    // 업체 담당자 사전 배정
    public void assignCompanyDeliveryMan(UUID companyDeliveryManId) {
        if (companyDeliveryManId == null) {
            throw new BusinessException(DeliveryErrorType.DELIVERYMAN_ID_REQUIRED);
        }
        if (this.companyDeliveryManId != null && !this.companyDeliveryManId.equals(companyDeliveryManId)) {
            throw new BusinessException(DeliveryErrorType.COMPANY_DELIVERYMAN_MISMATCH);
        }
        this.companyDeliveryManId = companyDeliveryManId;
    }

//    // 허브 출발 처리 (담당자 먼저 셋팅하고 출발하는거로 하는 경우)
//    public void startCompanyMoving(UUID companyDeliveryManId) {
//        if (this.status != DeliveryStatus.DEST_HUB_ARRIVED) {
//            throw new BusinessException(DeliveryErrorType.INVALID_STATUS_FOR_COMPANY_MOVING);
//        }
//        // 먼저 담당자 세팅
//        assignCompanyDeliveryMan(companyDeliveryManId);
//        // 그 다음 상태 전환
//        this.status = DeliveryStatus.COMPANY_MOVING;
//    }

    // 허브 출발 처리 / HUB_WAITING -> HUB_MOVING
    public void startHubMoving(int sequence, UUID deliveryManId) {
        if (deliveryManId == null) {
            throw new BusinessException(DeliveryErrorType.DELIVERYMAN_ID_REQUIRED);
        }
        if (status == DeliveryStatus.CANCELED
                || status == DeliveryStatus.DELIVERED
                || status == DeliveryStatus.COMPANY_MOVING) {
            throw new BusinessException(DeliveryErrorType.INVALID_STATUS_FOR_HUB_DEPARTURE);
        }

        DeliveryLog log = getLogBySequenceOrThrow(sequence);

        if (log.getStatus() != DeliveryLogStatus.HUB_WAITING) {
            throw new BusinessException(DeliveryErrorType.INVALID_LOG_STATUS_FOR_HUB_DEPARTURE);
        }

        // 🔹 허브 담당자 스냅샷 기록
        if (this.hubDeliveryManId == null) {
            this.hubDeliveryManId = deliveryManId;
        } else if (!this.hubDeliveryManId.equals(deliveryManId)) {
            // 정책에 따라 막을지, 허용할지.  지금은 막는 쪽으로.
            throw new BusinessException(DeliveryErrorType.HUB_DELIVERYMAN_MISMATCH);
        }

        // 로그 도메인 로직에 위임 (deliveryManId 세팅 + 상태 HUB_MOVING)
        log.start(deliveryManId);

        if (this.status == DeliveryStatus.HUB_WAITING) {
            this.status = DeliveryStatus.HUB_MOVING;
        }
        this.currentLogSeq = sequence;
    }

    // 허브 도착 처리 / HUB_MOVING -> HUB_ARRIVED or DEST_HUB_ARRIVED
    public void completeHubMoving(int sequence, double actualKm, int actualMinutes) {
        if (actualKm <= 0) {
            throw new BusinessException(DeliveryErrorType.ACTUAL_DISTANCE_MUST_BE_POSITIVE);
        }
        if (actualMinutes <= 0) {
            throw new BusinessException(DeliveryErrorType.ACTUAL_MINUTES_MUST_BE_POSITIVE);
        }
        if (status == DeliveryStatus.CANCELED || status == DeliveryStatus.DELIVERED) {
            throw new BusinessException(DeliveryErrorType.INVALID_STATUS_FOR_HUB_ARRIVAL);
        }

        DeliveryLog log = getLogBySequenceOrThrow(sequence);

        if (log.getStatus() != DeliveryLogStatus.HUB_MOVING) {
            throw new BusinessException(DeliveryErrorType.INVALID_LOG_STATUS_FOR_HUB_ARRIVAL);
        }

        log.complete(actualKm, actualMinutes);

        int maxSeq = logs.stream()
                .mapToInt(DeliveryLog::getSequence)
                .max()
                .orElse(sequence);

        if (sequence == maxSeq) {
            // 마지막 허브 leg 도착
            this.status = DeliveryStatus.DEST_HUB_ARRIVED;
            this.currentLogSeq = null;
        } else {
            // 중간 허브 도착: HUB_WAITING + 현재 위치는 sequence(도착 허브)
            this.status = DeliveryStatus.HUB_WAITING;
            this.currentLogSeq = sequence;
        }
    }

    // 목적지 허브 -> 업체 배송 시작
    public void startCompanyMoving(UUID companyDeliveryManId) {
        if (companyDeliveryManId == null) {
            throw new BusinessException(DeliveryErrorType.DELIVERYMAN_ID_REQUIRED);
        }
        if (this.status != DeliveryStatus.DEST_HUB_ARRIVED) {
            throw new BusinessException(DeliveryErrorType.INVALID_STATUS_FOR_COMPANY_MOVING);
        }

        this.companyDeliveryManId = companyDeliveryManId;
        this.status = DeliveryStatus.COMPANY_MOVING;
    }

    // 최종 업체 배송 완료
    public void completeDelivery() {
        if (this.status == DeliveryStatus.DELIVERED) {
            throw new BusinessException(DeliveryErrorType.DELIVERY_ALREADY_COMPLETED);
        }
        if (this.status != DeliveryStatus.COMPANY_MOVING) {
            throw new BusinessException(DeliveryErrorType.INVALID_STATUS_FOR_DELIVERY_COMPLETE);
        }
        this.status = DeliveryStatus.DELIVERED;
    }

    // 주문 취소 -> 배송 취소 / CREATED/HUB_WAITING 상태에서만 취소 가능
    public void cancel() {
        if (this.status != DeliveryStatus.CREATED
                && this.status != DeliveryStatus.HUB_WAITING) {
            throw new BusinessException(DeliveryErrorType.INVALID_STATUS_FOR_CANCEL);
        }

        boolean hasNotCancellableLog = logs.stream()
                .anyMatch(log ->
                        log.getStatus() != DeliveryLogStatus.CREATED
                                && log.getStatus() != DeliveryLogStatus.HUB_WAITING
                );

        if (hasNotCancellableLog) {
            throw new BusinessException(DeliveryErrorType.CANNOT_CANCEL_WHILE_LEG_IN_PROGRESS);
        }

        this.status = DeliveryStatus.CANCELED;

        for (DeliveryLog log : logs) {
            log.cancelFromDelivery();   // CREATED/HUB_WAITING -> CANCELED
        }
    }

}

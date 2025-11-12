package org.sparta.delivery.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.jpa.entity.BaseEntity;
import org.sparta.delivery.domain.enumeration.DeliveryStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_deliveries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Delivery extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryStatus deliveryStatus;

    @Column(nullable = false)
    private UUID departureHubId;

    @Column(nullable = false)
    private UUID destinationHubId;

    @Column(nullable = false, length = 500)
    private String deliveryAddress;

    @Column(nullable = false, length = 100)
    private String recipientName;

    @Column(length = 100)
    private String recipientSlackId;

    // 배송이 생성된 후 DeliveryMan 배정 (null 허용)
    private UUID companyDeliveryManId;

    // 배송이 생성된 후 DeliveryMan 배정 (null 허용)
    private UUID hubDeliveryManId;

    // Private 생성자
    private Delivery(
            UUID orderId,
            DeliveryStatus deliveryStatus,
            UUID departureHubId,
            UUID destinationHubId,
            String deliveryAddress,
            String recipientName,
            String recipientSlackId
    ) {
        this.orderId = orderId;
        this.deliveryStatus = deliveryStatus;
        this.departureHubId = departureHubId;
        this.destinationHubId = destinationHubId;
        this.deliveryAddress = deliveryAddress;
        this.recipientName = recipientName;
        this.recipientSlackId = recipientSlackId;
    }

    // ========================================
    // 정적 팩토리 메서드
    // ========================================

    /**
     * 배송 생성
     */
    public static Delivery create(
            UUID orderId,
            UUID departureHubId,
            UUID destinationHubId,
            String deliveryAddress,
            String recipientName,
            String recipientSlackId
    ) {
        return new Delivery(
                orderId,
                DeliveryStatus.HUB_WAITING,
                departureHubId,
                destinationHubId,
                deliveryAddress,
                recipientName,
                recipientSlackId
        );
    }

    // ========================================
    // 상태 변경 메서드
    // ========================================

    /**
     * 허브 대기 상태로 변경
     */
    public void hubWaiting() {
        this.deliveryStatus = DeliveryStatus.HUB_WAITING;
    }

    /**
     * 허브 이동 중 상태로 변경
     */
    public void hubMoving() {
        if (this.deliveryStatus != DeliveryStatus.HUB_WAITING) {
            throw new IllegalStateException("HUB_WAITING 상태에서만 허브 이동을 시작할 수 있습니다.");
        }
        this.deliveryStatus = DeliveryStatus.HUB_MOVING;
    }

    /**
     * 목적지 허브 도착 상태로 변경
     */
    public void arriveAtDestinationHub() {
        if (this.deliveryStatus != DeliveryStatus.HUB_MOVING) {
            throw new IllegalStateException("HUB_MOVING 상태에서만 도착할 수 있습니다.");
        }
        this.deliveryStatus = DeliveryStatus.DEST_HUB_ARRIVED;
    }

    /**
     * 업체 배송 시작 (업체 배송 담당자 배정)
     */
    public void startCompanyMoving(UUID companyDeliveryManId) {
        if (this.deliveryStatus != DeliveryStatus.HUB_WAITING &&
                this.deliveryStatus != DeliveryStatus.DEST_HUB_ARRIVED) {
            throw new IllegalStateException("업체 배송은 HUB_WAITING 또는 DEST_HUB_ARRIVED 상태에서만 시작할 수 있습니다.");
        }
        this.deliveryStatus = DeliveryStatus.COMPANY_MOVING;
        this.companyDeliveryManId = companyDeliveryManId;
    }

    /**
     * 배송 완료
     */
    public void completeDelivery() {
        if (this.deliveryStatus != DeliveryStatus.COMPANY_MOVING) {
            throw new IllegalStateException("배송은 COMPANY_MOVING 상태에서만 완료할 수 있습니다.");
        }
        this.deliveryStatus = DeliveryStatus.DELIVERED;
    }

    /**
     * 일반적인 상태 변경 (유연한 상태 변경)
     */
    public void updateStatus(DeliveryStatus deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    // ========================================
    // 배송 정보 변경 메서드
    // ========================================

    /**
     * 주소 변경 (주문이 PLACED일 때 변경 -> event 발행)
     */
    public void updateAddress(String newAddress) {
        this.deliveryAddress = newAddress;
    }

    /**
     * 업체 배송 담당자, 허브 배송 담당자 저장
     */
    public void saveDeliveryMan(UUID companyDeliveryManId, UUID hubDeliveryManId) {
        if (this.deliveryStatus == DeliveryStatus.DELIVERED || this.deliveryStatus == DeliveryStatus.CANCELED) {
            throw new IllegalStateException("완료되거나 취소된 배송은 담당자를 변경할 수 없습니다.");
        }
        this.companyDeliveryManId = companyDeliveryManId;
        this.hubDeliveryManId = hubDeliveryManId;
    }

    /**
     * 업체 배송 담당자만 배정
     */
    public void assignCompanyDeliveryMan(UUID companyDeliveryManId) {
        if (this.deliveryStatus == DeliveryStatus.DELIVERED || this.deliveryStatus == DeliveryStatus.CANCELED) {
            throw new IllegalStateException("완료되거나 취소된 배송은 담당자를 변경할 수 없습니다.");
        }
        this.companyDeliveryManId = companyDeliveryManId;
    }

    /**
     * 허브 배송 담당자만 배정
     */
    public void assignHubDeliveryMan(UUID hubDeliveryManId) {
        if (this.deliveryStatus == DeliveryStatus.DELIVERED || this.deliveryStatus == DeliveryStatus.CANCELED) {
            throw new IllegalStateException("완료되거나 취소된 배송은 담당자를 변경할 수 없습니다.");
        }
        this.hubDeliveryManId = hubDeliveryManId;
    }

    // ========================================
    // 논리 삭제 메서드
    // ========================================

    /**
     * 배송 삭제 (논리 삭제)
     */
    public void delete(UUID userId) {
        this.deletedAt = LocalDateTime.now();
    }

    // ========================================
    // 비즈니스 로직 검증 메서드
    // ========================================

    /**
     * 배송이 취소 가능한 상태인지 확인
     */
    public boolean isCancellable() {
        return this.deliveryStatus == DeliveryStatus.HUB_WAITING
                || this.deliveryStatus == DeliveryStatus.HUB_MOVING;
    }

    /**
     * 배송이 완료되었는지 확인
     */
    public boolean isCompleted() {
        return this.deliveryStatus == DeliveryStatus.DELIVERED;
    }

    /**
     * 배송 담당자가 배정되었는지 확인
     */
    public boolean hasDeliveryMan() {
        return this.companyDeliveryManId != null || this.hubDeliveryManId != null;
    }

    /**
     * 업체 배송 담당자가 배정되었는지 확인
     */
    public boolean hasCompanyDeliveryMan() {
        return this.companyDeliveryManId != null;
    }

    /**
     * 허브 배송 담당자가 배정되었는지 확인
     */
    public boolean hasHubDeliveryMan() {
        return this.hubDeliveryManId != null;
    }
}

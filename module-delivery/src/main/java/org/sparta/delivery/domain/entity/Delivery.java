package org.sparta.delivery.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.jpa.entity.BaseEntity;
import org.sparta.delivery.domain.enumeration.DeliveryStatus;

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
    
    private UUID companyDeliveryManId;  // 배송이 생성된 후 DeliveryMan 배정 null 허용해야 됨

    private UUID hubDeliveryManId;  // 배송이 생성된 후 DeliveryMan 배정 null 허용해야 됨


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

    // 배송 생성
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

    // 상태 변경 메서드
    public void hubWaiting() {
        this.deliveryStatus = DeliveryStatus.HUB_WAITING;
    }

    public void hubMoving() {
        this.deliveryStatus = DeliveryStatus.HUB_MOVING;
    }

    public void arriveAtDestinationHub() {
        this.deliveryStatus = DeliveryStatus.DEST_HUB_ARRIVED;
    }

    public void startCompanyMoving(UUID companyDeliveryManId) {
        this.deliveryStatus = DeliveryStatus.COMPANY_MOVING;
        this.companyDeliveryManId = companyDeliveryManId;
    }

    public void completeDelivery() {
        this.deliveryStatus = DeliveryStatus.DELIVERED;
    }

    // 주소 변경 (주문이 PLACED일 때 변경 -> event 발행)
    public void updateAddress(String newAddress) {
        this.deliveryAddress = newAddress;
    }

    // 업체 배송 담당자, 허브 배송 담당자 저장
    public void saveDeliveryMan(UUID companyDeliveryManId, UUID hubDeliveryManId) {
        this.companyDeliveryManId = companyDeliveryManId;
        this.hubDeliveryManId = hubDeliveryManId;
    }
}

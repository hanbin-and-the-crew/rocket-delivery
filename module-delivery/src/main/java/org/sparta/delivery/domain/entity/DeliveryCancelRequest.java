package org.sparta.delivery.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.delivery.domain.enumeration.CancelRequestStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 배송 취소 의도 (Cancel Intent) 엔티티
 * - 배송 생성 전 취소 이벤트가 도착했을 때, 취소 의도를 기록
 * - 배송 생성 시 이 테이블을 확인하여 유령 배송 방지
 */
@Entity
@Table(name = "p_delivery_cancel_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryCancelRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * 주문 ID (unique)
     * - 동일 주문에 대한 취소 의도는 1개만 존재
     */
    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    /**
     * 취소 이벤트 ID (unique)
     * - 멱등성 보장용
     */
    @Column(name = "cancel_event_id", nullable = false, unique = true)
    private UUID cancelEventId;

    /**
     * 취소 요청 상태
     * - REQUESTED: 취소 요청됨 (배송 생성 전)
     * - APPLIED: 취소 적용됨 (배송 취소 완료)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CancelRequestStatus status;

    // ===== Soft Delete 필드 =====

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ===== 팩토리 메서드 =====

    /**
     * 취소 요청 생성
     */
    public static DeliveryCancelRequest requested(UUID orderId, UUID cancelEventId) {
        DeliveryCancelRequest request = new DeliveryCancelRequest();
        request.orderId = orderId;
        request.cancelEventId = cancelEventId;
        request.status = CancelRequestStatus.REQUESTED;
        request.createdAt = LocalDateTime.now();
        return request;
    }

    // ===== 상태 변경 =====

    /**
     * 취소 적용 완료 처리
     */
    public void markApplied() {
        this.status = CancelRequestStatus.APPLIED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Soft Delete
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}

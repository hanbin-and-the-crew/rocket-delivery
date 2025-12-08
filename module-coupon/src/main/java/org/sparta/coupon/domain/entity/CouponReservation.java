package org.sparta.coupon.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.jpa.entity.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 쿠폰 예약 정보
 * - 쿠폰 예약 시 생성되며 5분간 유효
 * - 예약 만료 시 자동으로 정리
 */
@Entity
@Getter
@Table(
        name = "p_coupon_reservations",
        indexes = {
                @Index(name = "idx_reservations_coupon_id", columnList = "coupon_id"),
                @Index(name = "idx_reservations_order_id", columnList = "order_id"),
                @Index(name = "idx_reservations_expires_at", columnList = "expires_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponReservation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, name = "coupon_id")
    private UUID couponId;

    @Column(nullable = false, name = "order_id")
    private UUID orderId;

    @Column(nullable = false, name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    private Long orderAmount;

    @Column(nullable = false)
    private Long discountAmount;

    @Column(nullable = false)
    private LocalDateTime reservedAt;

    @Column(nullable = false, name = "expires_at")
    private LocalDateTime expiresAt;

    private CouponReservation(
            UUID couponId,
            UUID orderId,
            UUID userId,
            Long orderAmount,
            Long discountAmount,
            LocalDateTime reservedAt,
            LocalDateTime expiresAt
    ) {
        this.couponId = couponId;
        this.orderId = orderId;
        this.userId = userId;
        this.orderAmount = orderAmount;
        this.discountAmount = discountAmount;
        this.reservedAt = reservedAt;
        this.expiresAt = expiresAt;
    }

    /**
     * 쿠폰 예약 생성
     * - 예약 시간 5분 설정
     */
    public static CouponReservation create(
            UUID couponId,
            UUID orderId,
            UUID userId,
            Long orderAmount,
            Long discountAmount
    ) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(5);

        return new CouponReservation(
                couponId,
                orderId,
                userId,
                orderAmount,
                discountAmount,
                now,
                expiresAt
        );
    }

    /**
     * 예약 만료 여부 확인
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
}
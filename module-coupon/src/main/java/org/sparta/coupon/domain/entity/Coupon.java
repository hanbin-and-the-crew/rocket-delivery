package org.sparta.coupon.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.common.error.BusinessException;
import org.sparta.coupon.domain.enums.CouponStatus;
import org.sparta.coupon.domain.enums.DiscountType;
import org.sparta.coupon.domain.error.CouponErrorType;
import org.sparta.jpa.entity.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@Getter
@Table(
        name = "p_coupons",
        indexes = {
                @Index(name = "idx_coupons_code", columnList = "code"),
                @Index(name = "idx_coupons_user_id", columnList = "user_id"),
                @Index(name = "idx_coupons_status", columnList = "status")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscountType discountType;

    @Column(nullable = false)
    private Long discountAmount;

    @Column(nullable = false)
    private Long minOrderAmount;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponStatus status;

    @Column(nullable = false, name = "user_id")
    private UUID userId;

    @Column(name = "order_id")
    private UUID orderId;

    @Column
    private LocalDateTime usedAt;

    @Version
    private Long version;

    private Coupon(
            String code,
            String name,
            DiscountType discountType,
            Long discountAmount,
            Long minOrderAmount,
            LocalDateTime startDate,
            LocalDateTime endDate,
            UUID userId
    ) {
        this.code = code;
        this.name = name;
        this.discountType = discountType;
        this.discountAmount = discountAmount;
        this.minOrderAmount = minOrderAmount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.userId = userId;
        this.status = CouponStatus.AVAILABLE;
    }

    public static Coupon create(
            String code,
            String name,
            DiscountType discountType,
            Long discountAmount,
            Long minOrderAmount,
            LocalDateTime startDate,
            LocalDateTime endDate,
            UUID userId
    ) {
        validateCode(code);
        validateName(name);
        validateDiscountAmount(discountAmount);
        validateMinOrderAmount(minOrderAmount);
        validateDateRange(startDate, endDate);
        validateUserId(userId);

        return new Coupon(
                code,
                name,
                discountType,
                discountAmount,
                minOrderAmount,
                startDate,
                endDate,
                userId
        );
    }

    /**
     * 쿠폰 예약
     * - 상태 검증 및 RESERVED로 변경
     * - 만료 여부, 소유자, 최소 주문 금액 검증
     */
    public void reserve(UUID userId, UUID orderId, Long orderAmount) {
        validateOwner(userId);

        if (this.status != CouponStatus.AVAILABLE) {
            throw new BusinessException(CouponErrorType.COUPON_INVALID_STATUS);
        }

        if (isBeforeStart()) {
            throw new BusinessException(CouponErrorType.COUPON_NOT_STARTED);
        }

        if (isExpired()) {
            this.status = CouponStatus.EXPIRED;
            throw new BusinessException(CouponErrorType.COUPON_EXPIRED);
        }

        if (orderAmount < this.minOrderAmount) {
            throw new BusinessException(CouponErrorType.INSUFFICIENT_ORDER_AMOUNT);
        }

        this.status = CouponStatus.RESERVED;
        this.orderId = orderId;
    }

    /**
     * 쿠폰 사용 확정
     * - RESERVED → PAID 상태 변경
     * - 사용 일시 기록
     */
    public void confirm(UUID orderId) {
        if (this.status != CouponStatus.RESERVED) {
            throw new BusinessException(CouponErrorType.COUPON_INVALID_STATUS);
        }

        if (!this.orderId.equals(orderId)) {
            throw new BusinessException(CouponErrorType.INVALID_ORDER);
        }

        this.status = CouponStatus.PAID;
        this.usedAt = LocalDateTime.now();
    }

    /**
     * 쿠폰 예약 취소
     * - RESERVED → AVAILABLE 복원
     */
    public void cancelReservation() {
        if (this.status != CouponStatus.RESERVED) {
            throw new BusinessException(CouponErrorType.COUPON_INVALID_STATUS);
        }

        this.status = CouponStatus.AVAILABLE;
        this.orderId = null;
    }

    /**
     * 쿠폰 만료 처리
     */
    public void expire() {
        this.status = CouponStatus.EXPIRED;
    }

    /**
     * 쿠폰 만료 여부 확인
     */
    public boolean isExpired() {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(endDate);
    }

    private boolean isBeforeStart() {
        return LocalDateTime.now().isBefore(startDate);
    }

    /**
     * 할인 금액 계산
     */
    public Long calculateDiscount(Long orderAmount) {
        return switch (this.discountType) {
            case FIXED -> this.discountAmount;
            case PERCENTAGE -> orderAmount * this.discountAmount / 100;
        };
    }

    private static void validateCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("쿠폰 코드는 필수입니다");
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("쿠폰명은 필수입니다");
        }
    }

    private static void validateDiscountAmount(Long discountAmount) {
        if (discountAmount == null || discountAmount <= 0) {
            throw new IllegalArgumentException("할인 금액은 0보다 커야 합니다");
        }
    }

    private static void validateMinOrderAmount(Long minOrderAmount) {
        if (minOrderAmount == null || minOrderAmount < 0) {
            throw new IllegalArgumentException("최소 주문 금액은 0 이상이어야 합니다");
        }
    }

    private static void validateDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("유효기간은 필수입니다");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("시작일은 종료일보다 앞서야 합니다");
        }
    }

    private static void validateUserId(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다");
        }
    }

    private void validateOwner(UUID userId) {
        if (!this.userId.equals(userId)) {
            throw new BusinessException(CouponErrorType.USER_NOT_OWNER);
        }
    }
}

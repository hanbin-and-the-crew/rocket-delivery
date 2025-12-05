package org.sparta.coupon.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sparta.common.error.BusinessException;
import org.sparta.coupon.domain.enums.CouponStatus;
import org.sparta.coupon.domain.enums.DiscountType;
import org.sparta.coupon.domain.error.CouponErrorType;
import org.sparta.coupon.support.fixtures.CouponFixture;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Coupon 도메인 테스트")
class CouponTest {

    @Test
    @DisplayName("쿠폰을 정상적으로 생성하면 AVAILABLE 상태로 저장된다")
    void createCoupon_WithValidInput_ShouldSucceed() {
        
        Coupon coupon = CouponFixture.defaultCoupon();

        assertThat(coupon.getStatus()).isEqualTo(CouponStatus.AVAILABLE);
        assertThat(coupon.getCode()).isEqualTo("TEST-COUPON-001");
        assertThat(coupon.getName()).isEqualTo("테스트 쿠폰");
    }

    @Test
    @DisplayName("AVAILABLE 상태의 쿠폰을 예약하면 RESERVED 상태로 변경된다")
    void reserve_WithAvailableCoupon_ShouldChangeToReserved() {
        
        UUID userId = UUID.randomUUID();
        Coupon coupon = CouponFixture.withUserId(userId);
        UUID orderId = UUID.randomUUID();
        Long orderAmount = 50000L;

        coupon.reserve(userId, orderId, orderAmount);

        assertThat(coupon.getStatus()).isEqualTo(CouponStatus.RESERVED);
        assertThat(coupon.getOrderId()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("다른 사용자가 쿠폰을 예약하려고 하면 예외가 발생한다")
    void reserve_WithDifferentUser_ShouldThrowException() {
        
        UUID ownerId = UUID.randomUUID();
        UUID differentUserId = UUID.randomUUID();
        Coupon coupon = CouponFixture.withUserId(ownerId);
        UUID orderId = UUID.randomUUID();
        Long orderAmount = 50000L;

        
        assertThatThrownBy(() -> coupon.reserve(differentUserId, orderId, orderAmount))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorType", CouponErrorType.USER_NOT_OWNER);
    }

    @Test
    @DisplayName("만료된 쿠폰을 예약하려고 하면 예외가 발생한다")
    void reserve_WithExpiredCoupon_ShouldThrowException() {
        
        Coupon coupon = CouponFixture.expiredCoupon();
        UUID orderId = UUID.randomUUID();
        Long orderAmount = 50000L;

        
        assertThatThrownBy(() -> coupon.reserve(coupon.getUserId(), orderId, orderAmount))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorType", CouponErrorType.COUPON_EXPIRED);
    }

    @Test
    @DisplayName("시작일 이전 쿠폰을 예약하려고 하면 예외가 발생한다")
    void reserve_WithNotStartedCoupon_ShouldThrowException() {
        
        Coupon coupon = CouponFixture.notStartedCoupon();
        UUID orderId = UUID.randomUUID();
        Long orderAmount = 50000L;

        
        assertThatThrownBy(() -> coupon.reserve(coupon.getUserId(), orderId, orderAmount))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorType", CouponErrorType.COUPON_NOT_STARTED);
    }

    @Test
    @DisplayName("최소 주문 금액을 만족하지 않으면 예외가 발생한다")
    void reserve_WithInsufficientOrderAmount_ShouldThrowException() {
        
        Coupon coupon = CouponFixture.withMinOrderAmount(50000L);
        UUID orderId = UUID.randomUUID();
        Long orderAmount = 30000L; 

        
        assertThatThrownBy(() -> coupon.reserve(coupon.getUserId(), orderId, orderAmount))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorType", CouponErrorType.INSUFFICIENT_ORDER_AMOUNT);
    }

    @Test
    @DisplayName("이미 RESERVED 상태인 쿠폰을 다시 예약하려고 하면 예외가 발생한다")
    void reserve_WithAlreadyReservedCoupon_ShouldThrowException() {
        
        UUID userId = UUID.randomUUID();
        Coupon coupon = CouponFixture.withUserId(userId);
        UUID orderId1 = UUID.randomUUID();
        UUID orderId2 = UUID.randomUUID();
        Long orderAmount = 50000L;

        coupon.reserve(userId, orderId1, orderAmount); 

        
        assertThatThrownBy(() -> coupon.reserve(userId, orderId2, orderAmount))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorType", CouponErrorType.COUPON_INVALID_STATUS);
    }

    @Test
    @DisplayName("RESERVED 상태의 쿠폰을 확정하면 PAID 상태로 변경된다")
    void confirm_WithReservedCoupon_ShouldChangeToPaid() {
        
        UUID userId = UUID.randomUUID();
        Coupon coupon = CouponFixture.withUserId(userId);
        UUID orderId = UUID.randomUUID();
        Long orderAmount = 50000L;

        coupon.reserve(userId, orderId, orderAmount);

        coupon.confirm(orderId);

        assertThat(coupon.getStatus()).isEqualTo(CouponStatus.PAID);
        assertThat(coupon.getUsedAt()).isNotNull();
    }

    @Test
    @DisplayName("AVAILABLE 상태의 쿠폰을 확정하려고 하면 예외가 발생한다")
    void confirm_WithAvailableCoupon_ShouldThrowException() {
        
        Coupon coupon = CouponFixture.defaultCoupon();
        UUID orderId = UUID.randomUUID();

        assertThatThrownBy(() -> coupon.confirm(orderId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorType", CouponErrorType.COUPON_INVALID_STATUS);
    }

    @Test
    @DisplayName("다른 주문 ID로 쿠폰을 확정하려고 하면 예외가 발생한다")
    void confirm_WithDifferentOrderId_ShouldThrowException() {
        
        UUID userId = UUID.randomUUID();
        Coupon coupon = CouponFixture.withUserId(userId);
        UUID orderId = UUID.randomUUID();
        UUID differentOrderId = UUID.randomUUID();
        Long orderAmount = 50000L;

        coupon.reserve(userId, orderId, orderAmount);

        assertThatThrownBy(() -> coupon.confirm(differentOrderId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorType", CouponErrorType.INVALID_ORDER);
    }

    @Test
    @DisplayName("RESERVED 상태의 쿠폰 예약을 취소하면 AVAILABLE 상태로 복원된다")
    void cancelReservation_WithReservedCoupon_ShouldChangeToAvailable() {
        
        UUID userId = UUID.randomUUID();
        Coupon coupon = CouponFixture.withUserId(userId);
        UUID orderId = UUID.randomUUID();
        Long orderAmount = 50000L;

        coupon.reserve(userId, orderId, orderAmount);

        coupon.cancelReservation();

        assertThat(coupon.getStatus()).isEqualTo(CouponStatus.AVAILABLE);
        assertThat(coupon.getOrderId()).isNull();
    }

    @Test
    @DisplayName("AVAILABLE 상태의 쿠폰 예약을 취소하려고 하면 예외가 발생한다")
    void cancelReservation_WithAvailableCoupon_ShouldThrowException() {
        
        Coupon coupon = CouponFixture.defaultCoupon();

        assertThatThrownBy(() -> coupon.cancelReservation())
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorType", CouponErrorType.COUPON_INVALID_STATUS);
    }

    @Test
    @DisplayName("FIXED 타입 쿠폰은 고정 금액을 할인한다")
    void calculateDiscount_WithFixedType_ReturnsFixedAmount() {
        
        Coupon coupon = CouponFixture.withDiscountAmount(5000L);
        Long orderAmount = 50000L;

        Long discountAmount = coupon.calculateDiscount(orderAmount);

        assertThat(discountAmount).isEqualTo(5000L);
    }

    @Test
    @DisplayName("PERCENTAGE 타입 쿠폰은 주문 금액의 비율만큼 할인한다")
    void calculateDiscount_WithPercentageType_ReturnsPercentageAmount() {
        
        Coupon coupon = CouponFixture.percentageCoupon(); 
        Long orderAmount = 50000L;

        Long discountAmount = coupon.calculateDiscount(orderAmount);

        assertThat(discountAmount).isEqualTo(5000L); 
    }

    @ParameterizedTest
    @ValueSource(longs = {10000L, 20000L, 30000L})
    @DisplayName("다양한 주문 금액에서 PERCENTAGE 쿠폰이 정확히 계산된다")
    void calculateDiscount_WithVariousOrderAmounts_ReturnsCorrectAmount(Long orderAmount) {
        
        Coupon coupon = CouponFixture.percentageCoupon(); 

        Long discountAmount = coupon.calculateDiscount(orderAmount);

        assertThat(discountAmount).isEqualTo(orderAmount * 10 / 100);
    }

    @Test
    @DisplayName("쿠폰 만료 여부를 정확히 확인한다")
    void isExpired_WithExpiredCoupon_ReturnsTrue() {
        
        Coupon coupon = CouponFixture.expiredCoupon();

        boolean expired = coupon.isExpired();

        assertThat(expired).isTrue();
    }

    @Test
    @DisplayName("유효한 쿠폰의 만료 여부 확인 시 false를 반환한다")
    void isExpired_WithValidCoupon_ReturnsFalse() {
        
        Coupon coupon = CouponFixture.defaultCoupon();

        boolean expired = coupon.isExpired();
        
        assertThat(expired).isFalse();
    }
}

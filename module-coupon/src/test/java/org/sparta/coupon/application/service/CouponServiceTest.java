package org.sparta.coupon.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sparta.common.error.BusinessException;
import org.sparta.coupon.application.dto.CouponServiceResult;
import org.sparta.coupon.domain.entity.Coupon;
import org.sparta.coupon.domain.entity.CouponReservation;
import org.sparta.coupon.domain.enums.CouponStatus;
import org.sparta.coupon.domain.enums.DiscountType;
import org.sparta.coupon.domain.error.CouponErrorType;
import org.sparta.coupon.domain.repository.CouponRepository;
import org.sparta.coupon.domain.repository.CouponReservationRepository;
import org.sparta.coupon.infrastructure.redis.CouponReservationRedisManager;
import org.sparta.coupon.presentation.CouponRequest;
import org.sparta.coupon.support.fixtures.CouponFixture;
import org.sparta.coupon.support.fixtures.CouponReservationFixture;
import org.sparta.redis.util.DistributedLockExecutor;

import java.util.Optional;
import java.util.UUID;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CouponService 테스트")
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponReservationRepository couponReservationRepository;

    @Mock
    private DistributedLockExecutor lockExecutor;

    @Mock
    private CouponReservationRedisManager redisManager;

    @InjectMocks
    private CouponService couponService;

    @BeforeEach
    void setUp() {
        
        lenient().when(lockExecutor.executeWithLock(anyString(), any(Supplier.class))).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(1);
            return supplier.get();
        });
    }

    @Test
    @DisplayName("유효한 쿠폰을 예약하면 예약 정보가 반환된다")
    void reserveCoupon_WithValidCoupon_ReturnsReservation() {
        
        UUID couponId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Long orderAmount = 50000L;

        Coupon coupon = CouponFixture.withUserId(userId);
        CouponReservation reservation = CouponReservationFixture.defaultReservation();

        given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon));
        given(couponRepository.save(any(Coupon.class))).willAnswer(inv -> inv.getArgument(0));
        given(couponReservationRepository.save(any(CouponReservation.class))).willReturn(reservation);

        
        CouponServiceResult.Reserve result = couponService.reserveCoupon(
                reserveRequest(userId, orderId, orderAmount), couponId
        );

        
        assertThat(result).isNotNull();
        assertThat(result.reservationId()).isEqualTo(reservation.getId());
        assertThat(result.discountAmount()).isEqualTo(5000L); 
        assertThat(result.discountType()).isEqualTo(DiscountType.FIXED);
        assertThat(result.expiresAt()).isNotNull();

        verify(couponRepository).findById(couponId);
        verify(couponRepository).save(any(Coupon.class));
        verify(couponReservationRepository).save(any(CouponReservation.class));
    }

    @Test
    @DisplayName("존재하지 않는 쿠폰을 예약하려고 하면 예외가 발생한다")
    void reserveCoupon_WithNonExistentCoupon_ThrowsException() {
        
        UUID couponId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Long orderAmount = 50000L;

        given(couponRepository.findById(couponId)).willReturn(Optional.empty());

        
        assertThatThrownBy(() -> couponService.reserveCoupon(reserveRequest(userId, orderId, orderAmount), couponId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorType", CouponErrorType.COUPON_NOT_FOUND);

        verify(couponRepository).findById(couponId);
    }

    @Test
    @DisplayName("만료된 쿠폰을 예약하려고 하면 예외가 발생한다")
    void reserveCoupon_WithExpiredCoupon_ThrowsException() {
        
        UUID couponId = UUID.randomUUID();
        Coupon expiredCoupon = CouponFixture.expiredCoupon();
        UUID userId = expiredCoupon.getUserId();
        UUID orderId = UUID.randomUUID();
        Long orderAmount = 50000L;

        given(couponRepository.findById(couponId)).willReturn(Optional.of(expiredCoupon));

        
        assertThatThrownBy(() -> couponService.reserveCoupon(reserveRequest(userId, orderId, orderAmount), couponId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorType", CouponErrorType.COUPON_EXPIRED);

        verify(couponRepository).findById(couponId);
    }

    @Test
    @DisplayName("최소 주문 금액 미만으로 쿠폰을 예약하려고 하면 예외가 발생한다")
    void reserveCoupon_WithInsufficientOrderAmount_ThrowsException() {
        
        UUID couponId = UUID.randomUUID();
        Coupon coupon = CouponFixture.withMinOrderAmount(50000L);
        UUID userId = coupon.getUserId();
        UUID orderId = UUID.randomUUID();
        Long orderAmount = 30000L; 

        given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon));

        
        assertThatThrownBy(() -> couponService.reserveCoupon(reserveRequest(userId, orderId, orderAmount), couponId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorType", CouponErrorType.INSUFFICIENT_ORDER_AMOUNT);

        verify(couponRepository).findById(couponId);
    }

    @Test
    @DisplayName("쿠폰 사용을 확정하면 쿠폰이 PAID 상태로 변경되고 예약이 삭제된다")
    void confirmCoupon_WithValidReservation_SucceedsAndDeletesReservation() {
        
        UUID reservationId = UUID.randomUUID();
        UUID couponId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Coupon coupon = CouponFixture.withUserId(userId);
        coupon.reserve(userId, orderId, 50000L); 

        CouponReservation reservation = CouponReservationFixture.withCouponAndOrder(couponId, orderId);

        given(couponReservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));
        given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon));
        given(couponRepository.save(any(Coupon.class))).willAnswer(inv -> inv.getArgument(0));

        
        couponService.confirmCoupon(reservationId, orderId);

        
        assertThat(coupon.getStatus()).isEqualTo(CouponStatus.PAID);
        assertThat(coupon.getUsedAt()).isNotNull();

        verify(couponReservationRepository, times(2)).findById(reservationId);
        verify(couponRepository).findById(couponId);
        verify(couponRepository).save(any(Coupon.class));
        verify(couponReservationRepository).deleteById(reservationId);
    }

    @Test
    @DisplayName("존재하지 않는 예약을 확정하려고 하면 예외가 발생한다")
    void confirmCoupon_WithNonExistentReservation_ThrowsException() {
        
        UUID reservationId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        given(couponReservationRepository.findById(reservationId)).willReturn(Optional.empty());

        
        assertThatThrownBy(() -> couponService.confirmCoupon(reservationId, orderId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorType", CouponErrorType.RESERVATION_NOT_FOUND);

        verify(couponReservationRepository).findById(reservationId);
    }

    @Test
    @DisplayName("다른 주문 ID로 쿠폰 사용을 확정하려고 하면 예외가 발생한다")
    void confirmCoupon_WithDifferentOrderId_ThrowsException() {
        
        UUID reservationId = UUID.randomUUID();
        UUID couponId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID differentOrderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Coupon coupon = CouponFixture.withUserId(userId);
        coupon.reserve(userId, orderId, 50000L); 

        CouponReservation reservation = CouponReservationFixture.withCouponAndOrder(couponId, orderId);

        given(couponReservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));
        given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon));

        
        assertThatThrownBy(() -> couponService.confirmCoupon(reservationId, differentOrderId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorType", CouponErrorType.INVALID_ORDER);

        verify(couponReservationRepository, times(2)).findById(reservationId);
        verify(couponRepository).findById(couponId);
    }

    @Test
    @DisplayName("쿠폰 예약을 취소하면 쿠폰이 AVAILABLE 상태로 복원되고 예약이 삭제된다")
    void cancelReservation_WithValidReservation_SucceedsAndDeletesReservation() {
        
        UUID reservationId = UUID.randomUUID();
        UUID couponId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Coupon coupon = CouponFixture.withUserId(userId);
        coupon.reserve(userId, orderId, 50000L); 

        CouponReservation reservation = CouponReservationFixture.withCouponAndOrder(couponId, orderId);

        given(couponReservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));
        given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon));
        given(couponRepository.save(any(Coupon.class))).willAnswer(inv -> inv.getArgument(0));

        
        couponService.cancelReservation(reservationId);

        
        assertThat(coupon.getStatus()).isEqualTo(CouponStatus.AVAILABLE);
        assertThat(coupon.getOrderId()).isNull();

        verify(couponReservationRepository, times(2)).findById(reservationId);
        verify(couponRepository).findById(couponId);
        verify(couponRepository).save(any(Coupon.class));
        verify(couponReservationRepository).deleteById(reservationId);
    }

    @Test
    @DisplayName("존재하지 않는 예약을 취소하려고 하면 예외가 발생한다")
    void cancelReservation_WithNonExistentReservation_ThrowsException() {
        
        UUID reservationId = UUID.randomUUID();

        given(couponReservationRepository.findById(reservationId)).willReturn(Optional.empty());

        
        assertThatThrownBy(() -> couponService.cancelReservation(reservationId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorType", CouponErrorType.RESERVATION_NOT_FOUND);

        verify(couponReservationRepository).findById(reservationId);
    }

    private CouponRequest.Reserve reserveRequest(UUID userId, UUID orderId, Long orderAmount) {
        return new CouponRequest.Reserve(userId, orderId, orderAmount);
    }
}

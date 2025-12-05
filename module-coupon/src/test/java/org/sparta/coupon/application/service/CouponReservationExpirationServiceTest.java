package org.sparta.coupon.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sparta.coupon.domain.entity.Coupon;
import org.sparta.coupon.domain.entity.CouponReservation;
import org.sparta.coupon.domain.repository.CouponRepository;
import org.sparta.coupon.domain.repository.CouponReservationRepository;
import org.sparta.coupon.infrastructure.redis.CouponReservationRedisManager;
import org.sparta.coupon.support.fixtures.CouponFixture;
import org.sparta.redis.util.DistributedLockExecutor;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CouponReservationExpirationService 테스트")
class CouponReservationExpirationServiceTest {

    @Mock
    private CouponReservationRepository couponReservationRepository;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponReservationRedisManager redisManager;

    @Mock
    private DistributedLockExecutor lockExecutor;

    @Mock
    private ApplicationContext applicationContext;

    @InjectMocks
    private CouponReservationExpirationService expirationService;

    @BeforeEach
    void setUp() {
        lenient().when(lockExecutor.executeWithLock(anyString(), any(Supplier.class))).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(1);
            return supplier.get();
        });

        lenient().when(applicationContext.getBean(CouponReservationExpirationService.class))
                .thenReturn(expirationService);
    }

    @Test
    @DisplayName("예약 정보가 없으면 Redis 키만 정리한다")
    void handleExpiredReservation_WhenReservationMissing_DeletesRedisOnly() {
        UUID reservationId = UUID.randomUUID();
        when(couponReservationRepository.findById(reservationId)).thenReturn(Optional.empty());

        expirationService.handleExpiredReservation(reservationId);

        verify(redisManager).deleteReservation(reservationId);
        verify(lockExecutor, never()).executeWithLock(anyString(), any(Supplier.class));
    }

    @Test
    @DisplayName("만료되지 않은 예약은 정리하지 않는다")
    void handleExpiredReservation_WhenNotExpired_DoesNothing() {
        UUID reservationId = UUID.randomUUID();
        UUID couponId = UUID.randomUUID();
        CouponReservation reservation = mock(CouponReservation.class);

        when(reservation.getCouponId()).thenReturn(couponId);
        when(reservation.isExpired()).thenReturn(false);
        lenient().when(couponReservationRepository.findById(reservationId))
                .thenReturn(Optional.of(reservation));

        expirationService.handleExpiredReservation(reservationId);

        verify(couponReservationRepository, times(2)).findById(reservationId);
        verify(couponRepository, never()).findById(any());
        verify(couponReservationRepository, never()).deleteById(any());
        verify(redisManager, never()).deleteReservation(reservationId);
    }

    @Test
    @DisplayName("만료된 예약은 쿠폰 상태를 복구하고 데이터도 정리한다")
    void handleExpiredReservation_WhenExpired_ReleasesCoupon() {
        UUID reservationId = UUID.randomUUID();
        UUID couponId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        CouponReservation reservation = mock(CouponReservation.class);
        when(reservation.getCouponId()).thenReturn(couponId);
        when(reservation.isExpired()).thenReturn(true);
        lenient().when(couponReservationRepository.findById(reservationId))
                .thenReturn(Optional.of(reservation));

        Coupon coupon = CouponFixture.withUserId(userId);
        ReflectionTestUtils.setField(coupon, "id", couponId);
        coupon.reserve(userId, orderId, 50000L);

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));

        expirationService.handleExpiredReservation(reservationId);

        verify(couponReservationRepository, times(2)).findById(reservationId);
        verify(couponRepository).save(coupon);
        verify(couponReservationRepository).deleteById(reservationId);
        verify(redisManager).deleteReservation(reservationId);
    }

    @Test
    @DisplayName("배치 실행 시 만료 예약 목록을 조회한다")
    void handleExpiredReservations_BatchFetchesExpiredList() {
        UUID reservationId = UUID.randomUUID();
        CouponReservation reservation = mock(CouponReservation.class);
        when(reservation.getId()).thenReturn(reservationId);

        when(couponReservationRepository.findExpiredReservations(any(LocalDateTime.class), anyInt()))
                .thenReturn(List.of(reservation));

        when(couponReservationRepository.findById(reservationId))
                .thenReturn(Optional.of(reservation));
        when(reservation.getCouponId()).thenReturn(UUID.randomUUID());
        when(reservation.isExpired()).thenReturn(false);

        expirationService.handleExpiredReservations(LocalDateTime.now(), 10);

        verify(couponReservationRepository).findExpiredReservations(any(LocalDateTime.class), eq(10));
    }
}

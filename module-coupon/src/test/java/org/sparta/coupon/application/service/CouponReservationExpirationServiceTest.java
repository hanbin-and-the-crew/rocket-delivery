/*
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

    */
/**
     * 【만료 처리 - Redis TTL 먼저 만료된 경우】
     *
     * 기능: Redis TTL이 먼저 만료되어 DB에 예약 정보가 없는 경우 Redis 키만 정리
     *
     * 시나리오:
     * 1. Redis TTL(5분) 만료 이벤트 발생
     * 2. DB 조회 시 예약 정보 없음 (이미 삭제됨)
     * 3. Redis 키만 삭제하고 종료
     *
     * 성공 흐름:
     * - Redis 키 삭제됨
     * - 분산 락 획득 시도 안 함 (불필요)
     *
     * 검증 포인트:
     * ✓ 예약 정보 없을 때 안전한 처리
     * ✓ Redis 고아 키 정리
     *//*

    @Test
    @DisplayName("예약 정보가 없으면 Redis 키만 정리한다")
    void handleExpiredReservation_WhenReservationMissing_DeletesRedisOnly() {
        UUID reservationId = UUID.randomUUID();
        when(couponReservationRepository.findById(reservationId)).thenReturn(Optional.empty());

        expirationService.handleExpiredReservation(reservationId);

        verify(redisManager).deleteReservation(reservationId);
        verify(lockExecutor, never()).executeWithLock(anyString(), any(Supplier.class));
    }

    */
/**
     * 【만료 처리 - 아직 만료 안 된 경우】
     *
     * 기능: 만료 시간이 안 된 예약은 정리하지 않음
     *
     * 시나리오:
     * 1. 예약 정보 조회
     * 2. 만료 여부 체크 (isExpired() → false)
     * 3. 만료 안 됐으므로 아무 작업도 하지 않음
     *
     * 성공 흐름:
     * - 쿠폰 조회 안 함
     * - DB/Redis 삭제 안 함
     *
     * 검증 포인트:
     * ✓ 만료 시간 정확히 체크
     * ✓ 유효한 예약은 보존
     *//*

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

    */
/**
     * 【만료 처리 - 5분 경과 후 쿠폰 복구】
     *
     * 기능: 5분 경과한 예약은 쿠폰을 AVAILABLE로 복구하고 데이터 정리
     *
     * 시나리오:
     * 1. 예약 후 5분 경과 (TTL 만료)
     * 2. Redis 만료 이벤트 발생
     * 3. 분산 락 획득
     * 4. 쿠폰 상태 확인 (RESERVED)
     * 5. 쿠폰 상태 복구 (RESERVED → AVAILABLE)
     * 6. DB/Redis에서 예약 정보 삭제
     *
     * 성공 흐름:
     * - 쿠폰 상태: RESERVED → AVAILABLE
     * - DB 예약 정보 삭제
     * - Redis 캐시 삭제
     *
     * 검증 포인트:
     * ✓ 5분 만료 시 자동 복구
     * ✓ 다른 사용자가 재예약 가능
     * ✓ 분산 락으로 동시성 제어
     *//*

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

    */
/**
     * 【만료 처리 - 배치 스케줄러】
     *
     * 기능: 주기적으로 만료된 예약 목록 조회 및 일괄 처리
     *
     * 시나리오:
     * 1. 스케줄러가 60초마다 실행
     * 2. 만료된 예약 목록 조회 (배치 사이즈 10)
     * 3. 각 예약에 대해 개별 처리
     *
     * 성공 흐름:
     * - findExpiredReservations 호출됨
     * - 배치 사이즈 파라미터 전달 확인
     *
     * 검증 포인트:
     * ✓ 배치 조회 정상 동작
     * ✓ Redis TTL 이벤트 보완 (이중 안전장치)
     *//*

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
*/

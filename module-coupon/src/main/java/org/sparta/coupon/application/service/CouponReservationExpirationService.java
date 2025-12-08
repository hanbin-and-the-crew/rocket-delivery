package org.sparta.coupon.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.error.BusinessException;
import org.sparta.coupon.domain.entity.Coupon;
import org.sparta.coupon.domain.entity.CouponReservation;
import org.sparta.coupon.domain.enums.CouponStatus;
import org.sparta.coupon.domain.repository.CouponRepository;
import org.sparta.coupon.domain.repository.CouponReservationRepository;
import org.sparta.coupon.infrastructure.redis.CouponReservationRedisManager;
import org.sparta.redis.util.DistributedLockExecutor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * 만료된 쿠폰 예약 정리 서비스
 * - TTL 만료 및 배치를 통해 쿠폰 상태를 AVAILABLE로 복구
 * - DB/Redis 예약 데이터를 정리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponReservationExpirationService {

    private static final String LOCK_KEY_PREFIX = "coupon:lock:";

    private final CouponReservationRepository couponReservationRepository;
    private final CouponRepository couponRepository;
    private final CouponReservationRedisManager redisManager;
    private final DistributedLockExecutor lockExecutor;
    private final ApplicationContext applicationContext;

    /**
     * 만료된 예약을 정리
     * - 분산 락 획득 후 트랜잭션 시작
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleExpiredReservation(UUID reservationId) {
        Optional<CouponReservation> reservationOpt = couponReservationRepository.findById(reservationId);

        if (reservationOpt.isEmpty()) {
            // Redis TTL이 먼저 만료되어 예약이 이미 삭제된 경우 Redis 키만 정리
            redisManager.deleteReservation(reservationId);
            return;
        }

        CouponReservation reservation = reservationOpt.get();
        String lockKey = LOCK_KEY_PREFIX + reservation.getCouponId();

        lockExecutor.executeWithLock(lockKey, () -> {
            processReservation(reservationId);
            return null;
        });
    }

    @Transactional
    private void processReservation(UUID reservationId) {
        CouponReservation reservation = couponReservationRepository.findById(reservationId)
                .orElse(null);

        if (reservation == null) {
            redisManager.deleteReservation(reservationId);
            return;
        }

        if (!reservation.isExpired()) {
            log.debug("Reservation 아직 만료되지 않음: reservationId={}", reservationId);
            return;
        }

        recoverCouponIfReserved(reservation);
        deleteReservation(reservationId);
    }

    private void recoverCouponIfReserved(CouponReservation reservation) {
        couponRepository.findById(reservation.getCouponId())
                .ifPresentOrElse(
                        coupon -> {
                            if (coupon.getStatus() == CouponStatus.RESERVED) {
                                try {
                                    coupon.cancelReservation();
                                    couponRepository.save(coupon);
                                    log.info("만료된 예약으로 쿠폰 상태 복구: couponId={}, reservationId={}",
                                            coupon.getId(), reservation.getId());
                                } catch (BusinessException e) {
                                    log.warn("쿠폰 상태 복구 중 비즈니스 예외 발생: couponId={}, error={}",
                                            coupon.getId(), e.getErrorType(), e);
                                }
                            }
                        },
                        () -> log.warn("예약에 해당하는 쿠폰을 찾지 못했습니다: couponId={}, reservationId={}",
                                reservation.getCouponId(), reservation.getId())
                );
    }

    private void deleteReservation(UUID reservationId) {
        couponReservationRepository.deleteById(reservationId);
        redisManager.deleteReservation(reservationId);
        log.debug("만료 예약 데이터 삭제 완료: reservationId={}", reservationId);
    }

    /**
     * 특정 시각 이전에 만료된 예약 배치 처리
     * - 각 예약은 독립적인 트랜잭션으로 처리 (하나의 실패가 전체에 영향 없음)
     * 트랜잭션 전파 보장
     */
    public void handleExpiredReservations(LocalDateTime referenceTime, int batchSize) {
        CouponReservationExpirationService self = applicationContext.getBean(CouponReservationExpirationService.class);

        couponReservationRepository.findExpiredReservations(referenceTime, batchSize)
                .forEach(reservation -> {
                    try {
                        self.handleExpiredReservation(reservation.getId());
                    } catch (Exception e) {
                        log.error("만료 예약 처리 실패: reservationId={}", reservation.getId(), e);
                    }
                });
    }
}

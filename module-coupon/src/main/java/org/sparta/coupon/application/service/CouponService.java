package org.sparta.coupon.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.error.BusinessException;
import org.sparta.coupon.application.dto.CouponServiceResult;
import org.sparta.coupon.domain.entity.Coupon;
import org.sparta.coupon.domain.entity.CouponReservation;
import org.sparta.coupon.domain.error.CouponErrorType;
import org.sparta.coupon.domain.repository.CouponRepository;
import org.sparta.coupon.domain.repository.CouponReservationRepository;
import org.sparta.coupon.infrastructure.redis.CouponReservationCacheInfo;
import org.sparta.coupon.infrastructure.redis.CouponReservationRedisManager;
import org.sparta.coupon.presentation.CouponRequest;
import org.sparta.redis.util.DistributedLockExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Coupon 서비스
 * - 쿠폰 검증, 예약, 사용 확정, 취소 담당
 * - ECS 다중 태스크 환경을 위한 Redis 분산 락 적용
 * - 낙관적 락(@Version)으로 2차 방어
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponReservationRepository couponReservationRepository;
    private final DistributedLockExecutor lockExecutor;
    private final CouponReservationRedisManager redisManager;

    /**
     * 쿠폰 검증 및 예약
     * - 분산 락으로 ECS 다중 태스크 환경에서 동시성 제어
     * - @Version으로 낙관적 락 적용
     * - AVAILABLE → RESERVED 상태 변경
     * - CouponReservation 생성 (5분 만료)
     */
    @Transactional
    public CouponServiceResult.Reserve reserveCoupon(CouponRequest.Reserve request, UUID couponId) {
        return executeWithCouponLock(couponId, () -> processCouponReservation(request, couponId));
    }

    /**
     * 쿠폰 사용 확정
     * - 예약 존재 및 유효성 확인
     * - RESERVED → PAID 상태 변경
     * - 사용 일시 기록
     */
    @Transactional
    public void confirmCoupon(UUID reservationId, UUID orderId) {
        // 1. 예약 정보 조회
        CouponReservation reservation = couponReservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException(CouponErrorType.RESERVATION_NOT_FOUND));

        // 2. 예약 만료 확인
        if (reservation.isExpired()) {
            throw new BusinessException(CouponErrorType.RESERVATION_EXPIRED);
        }

        // 3. 쿠폰 조회 및 사용 확정
        Coupon coupon = couponRepository.findById(reservation.getCouponId())
                .orElseThrow(() -> new BusinessException(CouponErrorType.COUPON_NOT_FOUND));

        coupon.confirm(orderId);
        couponRepository.save(coupon);

        // 4. 예약 정보 삭제 (DB + Redis)
        couponReservationRepository.deleteById(reservationId);
        redisManager.deleteReservation(reservationId);

        log.info("쿠폰 사용 확정 완료: reservationId={}, couponId={}, orderId={}",
                reservationId, reservation.getCouponId(), orderId);
    }

    /**
     * 쿠폰 예약 취소
     * - RESERVED → AVAILABLE 복원
     * - 예약 정보 삭제
     *
     * @param reservationId 예약 ID
     */
    @Transactional
    public void cancelReservation(UUID reservationId) {
        // 1. 예약 정보 조회
        CouponReservation reservation = couponReservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException(CouponErrorType.RESERVATION_NOT_FOUND));

        // 2. 쿠폰 조회 및 예약 취소
        Coupon coupon = couponRepository.findById(reservation.getCouponId())
                .orElseThrow(() -> new BusinessException(CouponErrorType.COUPON_NOT_FOUND));

        coupon.cancelReservation();
        couponRepository.save(coupon);

        // 3. 예약 정보 삭제 (DB + Redis)
        couponReservationRepository.deleteById(reservationId);
        redisManager.deleteReservation(reservationId);

        log.info("쿠폰 예약 취소 완료: reservationId={}, couponId={}",
                reservationId, reservation.getCouponId());
    }


    private CouponServiceResult.Reserve processCouponReservation(CouponRequest.Reserve request, UUID couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException(CouponErrorType.COUPON_NOT_FOUND));

        coupon.reserve(request.userId(), request.orderId(), request.orderAmount());
        couponRepository.save(coupon);

        Long discountAmount = coupon.calculateDiscount(request.orderAmount());

        CouponReservation savedReservation = couponReservationRepository.save(
                CouponReservation.create(
                        couponId,
                        request.orderId(),
                        request.userId(),
                        request.orderAmount(),
                        discountAmount
                )
        );

        redisManager.saveReservation(
                savedReservation.getId(),
                new CouponReservationCacheInfo(
                        savedReservation.getId(),
                        couponId,
                        savedReservation.getOrderId(),
                        savedReservation.getUserId()
                )
        );

        log.info("쿠폰 예약 완료: couponId={}, reservationId={}",
                couponId, savedReservation.getId());

        return new CouponServiceResult.Reserve(
                savedReservation.getId(),
                discountAmount,
                coupon.getDiscountType(),
                savedReservation.getExpiresAt()
        );
    }

    private <T> T executeWithCouponLock(UUID couponId, Supplier<T> action) {
        String lockKey = "coupon:lock:" + couponId;
        return lockExecutor.executeWithLock(lockKey, action);
    }

}

package org.sparta.coupon.infrastructure.jpa;

import org.sparta.coupon.domain.entity.CouponReservation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * CouponReservation JPA Repository
 * - 쿠폰 예약 데이터 접근
 */
public interface CouponReservationJpaRepository extends JpaRepository<CouponReservation, UUID> {

    Optional<CouponReservation> findByCouponIdAndOrderId(UUID couponId, UUID orderId);

    List<CouponReservation> findByExpiresAtBefore(LocalDateTime referenceTime, Pageable pageable);
}

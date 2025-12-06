package org.sparta.coupon.infrastructure.jpa;

import org.sparta.coupon.domain.entity.CouponReservation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CouponReservationJpaRepository extends JpaRepository<CouponReservation, UUID> {

    Optional<CouponReservation> findByOrderId(UUID orderId);

    Optional<CouponReservation> findByCouponIdAndOrderId(UUID couponId, UUID orderId);

    List<CouponReservation> findByExpiresAtBefore(LocalDateTime referenceTime, Pageable pageable);
}

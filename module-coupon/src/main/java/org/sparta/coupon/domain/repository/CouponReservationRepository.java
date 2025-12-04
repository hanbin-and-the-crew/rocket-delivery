package org.sparta.coupon.domain.repository;

import org.sparta.coupon.domain.entity.CouponReservation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CouponReservationRepository {

    CouponReservation save(CouponReservation couponReservation);

    Optional<CouponReservation> findById(UUID id);

    Optional<CouponReservation> findByCouponIdAndOrderId(UUID couponId, UUID orderId);

    List<CouponReservation> findExpiredReservations(LocalDateTime referenceTime, int limit);

    void deleteById(UUID id);

    long count();

    boolean existsById(UUID id);
}

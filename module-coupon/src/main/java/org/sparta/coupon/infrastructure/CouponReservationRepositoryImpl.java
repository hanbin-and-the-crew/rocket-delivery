package org.sparta.coupon.infrastructure;

import lombok.RequiredArgsConstructor;
import org.sparta.coupon.domain.entity.CouponReservation;
import org.sparta.coupon.domain.repository.CouponReservationRepository;
import org.sparta.coupon.infrastructure.jpa.CouponReservationJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CouponReservationRepositoryImpl implements CouponReservationRepository {

    private final CouponReservationJpaRepository couponReservationJpaRepository;

    @Override
    public CouponReservation save(CouponReservation couponReservation) {
        return couponReservationJpaRepository.save(couponReservation);
    }

    @Override
    public Optional<CouponReservation> findById(UUID id) {
        return couponReservationJpaRepository.findById(id);
    }

    @Override
    public Optional<CouponReservation> findByOrderId(UUID orderId) {
        return couponReservationJpaRepository.findByOrderId(orderId);
    }

    @Override
    public Optional<CouponReservation> findByCouponIdAndOrderId(UUID couponId, UUID orderId) {
        return couponReservationJpaRepository.findByCouponIdAndOrderId(couponId, orderId);
    }

    @Override
    public List<CouponReservation> findExpiredReservations(LocalDateTime referenceTime, int limit) {
        return couponReservationJpaRepository.findByExpiresAtBefore(referenceTime, PageRequest.of(0, limit));
    }

    @Override
    public void deleteById(UUID id) {
        couponReservationJpaRepository.deleteById(id);
    }

    @Override
    public long count() {
        return couponReservationJpaRepository.count();
    }

    @Override
    public boolean existsById(UUID id) {
        return couponReservationJpaRepository.existsById(id);
    }
}

package org.sparta.coupon.infrastructure;

import lombok.RequiredArgsConstructor;
import org.sparta.coupon.domain.entity.Coupon;
import org.sparta.coupon.domain.repository.CouponRepository;
import org.sparta.coupon.infrastructure.jpa.CouponJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CouponRepositoryImpl implements CouponRepository {

    private final CouponJpaRepository couponJpaRepository;

    @Override
    public Coupon save(Coupon coupon) {
        return couponJpaRepository.save(coupon);
    }

    @Override
    public Optional<Coupon> findById(UUID id) {
        return couponJpaRepository.findById(id);
    }

    @Override
    public Optional<Coupon> findByCode(String code) {
        return couponJpaRepository.findByCode(code);
    }
}
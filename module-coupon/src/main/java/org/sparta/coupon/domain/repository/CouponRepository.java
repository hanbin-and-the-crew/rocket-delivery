package org.sparta.coupon.domain.repository;

import org.sparta.coupon.domain.entity.Coupon;

import java.util.Optional;
import java.util.UUID;

public interface CouponRepository {

    Coupon save(Coupon coupon);

    Optional<Coupon> findById(UUID id);

    Optional<Coupon> findByCode(String code);
}
package org.sparta.coupon.infrastructure.jpa;

import org.sparta.coupon.domain.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Coupon JPA Repository
 * - 쿠폰 데이터 접근
 */
public interface CouponJpaRepository extends JpaRepository<Coupon, UUID> {

    Optional<Coupon> findByCode(String code);
}
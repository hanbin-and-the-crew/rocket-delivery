package org.sparta.coupon.application.boot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.coupon.domain.entity.Coupon;
import org.sparta.coupon.domain.enums.DiscountType;
import org.sparta.coupon.domain.repository.CouponRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponDataSeedService {

    private static final String SAMPLE_COUPON_CODE = "WELCOME-10000";
    private static final UUID SAMPLE_USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private final CouponRepository couponRepository;

    @Transactional
    public void seedSampleCoupon() {
        boolean exists = couponRepository.findByCode(SAMPLE_COUPON_CODE).isPresent();

        if (exists) {
            log.info("기존 샘플 쿠폰({})이 존재하여 시딩을 건너뜁니다.", SAMPLE_COUPON_CODE);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        Coupon coupon = Coupon.create(
                SAMPLE_COUPON_CODE,
                "신규 가입 웰컴 쿠폰",
                DiscountType.FIXED,
                10_000L,
                30_000L,
                now.minusDays(1),
                now.plusMonths(1),
                SAMPLE_USER_ID
        );

        couponRepository.save(coupon);
        log.info("샘플 쿠폰({}) 1건을 생성했습니다.", SAMPLE_COUPON_CODE);
    }
}

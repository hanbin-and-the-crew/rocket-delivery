package org.sparta.coupon.application.boot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile({"local", "dev"})
@RequiredArgsConstructor
public class CouponDataSeeder implements CommandLineRunner {

    private final CouponDataSeedService couponDataSeedService;

    @Override
    public void run(String... args) {
        log.info("Coupon 모듈 샘플 데이터 시딩을 시작합니다.");
        couponDataSeedService.seedSampleCoupon();
    }
}

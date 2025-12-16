package org.sparta.coupon.application.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.coupon.application.service.CouponReservationExpirationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 만료된 쿠폰 예약을 주기적으로 정리하는 스케줄러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponReservationExpirationScheduler {

    private final CouponReservationExpirationService expirationService;

    @Value("${coupon.reservation.expiration.batch-size:100}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${coupon.reservation.expiration.fixed-delay-ms:60000}")
    public void cleanUpExpiredReservations() {
        log.debug("만료 쿠폰 예약 정리 스케줄 시작");
        expirationService.handleExpiredReservations(LocalDateTime.now(), batchSize);
    }
}

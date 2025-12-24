/*
package org.sparta.coupon.application.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.coupon.TestContainersConfig;
import org.sparta.coupon.domain.entity.Coupon;
import org.sparta.coupon.domain.enums.DiscountType;
import org.sparta.coupon.domain.repository.CouponRepository;
import org.sparta.coupon.domain.repository.CouponReservationRepository;
import org.sparta.coupon.presentation.CouponRequest;
import org.sparta.redis.util.LockAcquisitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;


@DisplayName("CouponService 분산 락 동시성 테스트")
class CouponServiceConcurrencyTest extends TestContainersConfig {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponReservationRepository couponReservationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private UUID testUserId;
    private UUID testCouponId;

    @BeforeEach
    void setUp() {
        
        jdbcTemplate.execute("DELETE FROM p_coupon_reservations");
        jdbcTemplate.execute("DELETE FROM p_coupons");

        testUserId = UUID.randomUUID();

        
        testCouponId = transactionTemplate.execute(status -> {
            Coupon coupon = Coupon.create(
                    "CONC-" + System.currentTimeMillis(),
                    "동시성 테스트 쿠폰",
                    DiscountType.FIXED,
                    5000L,
                    10000L,
                    LocalDateTime.now().minusDays(1),
                    LocalDateTime.now().plusDays(30),
                    testUserId
            );
            return couponRepository.save(coupon).getId();
        });
    }

    @AfterEach
    void tearDown() {
        
        jdbcTemplate.execute("DELETE FROM p_coupon_reservations");
        jdbcTemplate.execute("DELETE FROM p_coupons");
    }

    */
/**
     * 【동시성 제어 테스트 - 분산락 검증】
     *
     * 기능: 하나의 쿠폰에 대해 동시에 여러 예약 요청이 들어올 때 분산락으로 동시성 제어
     *
     * 시나리오:
     * 1. 10개의 스레드가 동시에 같은 쿠폰을 예약 시도
     * 2. Redis 분산락(Redisson)으로 쿠폰별 락 획득 경쟁
     * 3. 첫 번째 스레드만 락 획득 → 예약 성공 (AVAILABLE → RESERVED)
     * 4. 나머지 9개 스레드는 락 획득 실패 → LockAcquisitionException 또는 비즈니스 예외
     *
     * 성공 흐름:
     * - 성공 카운트: 1개
     * - 실패 카운트: 9개 (락 획득 실패 + 기타 예외)
     * - DB 예약 건수: 1건
     * - 쿠폰 상태: RESERVED
     *
     * 검증 포인트:
     * ✓ 분산락이 정상 동작하여 동시 요청 중 단 1개만 성공
     * ✓ Race Condition 없이 데이터 정합성 보장
     *//*

    @Test
    @DisplayName("같은 쿠폰에 대한 10개 동시 예약 요청 시 1개만 성공한다")
    void reserveCoupon_ConcurrentRequests_OnlyOneSucceeds() throws InterruptedException {
        
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger lockFailureCount = new AtomicInteger(0);
        AtomicInteger otherFailureCount = new AtomicInteger(0);

        List<UUID> orderIds = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            orderIds.add(UUID.randomUUID());
        }

        
        for (int i = 0; i < threadCount; i++) {
            final UUID orderId = orderIds.get(i);
            executorService.submit(() -> {
                try {
                    couponService.reserveCoupon(
                            reserveRequest(testUserId, orderId, 50000L),
                            testCouponId
                    );
                    successCount.incrementAndGet();
                } catch (LockAcquisitionException e) {
                    
                    lockFailureCount.incrementAndGet();
                } catch (Exception e) {
                    
                    otherFailureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        assertThat(successCount.get()).isEqualTo(1); 
        assertThat(lockFailureCount.get() + otherFailureCount.get()).isEqualTo(9); 

        long reservationCount = couponReservationRepository.count();
        assertThat(reservationCount).isEqualTo(1);

        Coupon coupon = couponRepository.findById(testCouponId).orElseThrow();
        assertThat(coupon.getStatus()).isEqualTo(org.sparta.coupon.domain.enums.CouponStatus.RESERVED);
    }

    */
/**
     * 【병렬 처리 성능 테스트 - 락 격리성 검증】
     *
     * 기능: 서로 다른 쿠폰에 대한 예약은 독립적인 락으로 병렬 처리 가능
     *
     * 시나리오:
     * 1. 5개의 서로 다른 쿠폰 생성
     * 2. 각 쿠폰에 대해 동시에 예약 요청 (총 5개 스레드)
     * 3. 각 쿠폰은 독립적인 락 키를 가짐 (coupon:lock:{couponId})
     * 4. 모든 스레드가 서로 다른 락을 획득하므로 블로킹 없이 병렬 처리
     *
     * 성공 흐름:
     * - 성공 카운트: 5개 (모두 성공)
     * - 각 쿠폰 상태: 모두 RESERVED
     *
     * 검증 포인트:
     * ✓ 쿠폰별 락 격리성 - 서로 다른 쿠폰은 락 경쟁 없음
     * ✓ 분산 환경에서 병렬 처리 성능 확보
     *//*

    @Test
    @DisplayName("서로 다른 쿠폰에 대한 동시 예약은 모두 성공한다")
    void reserveCoupon_DifferentCoupons_AllSucceed() throws InterruptedException {
        
        int couponCount = 5;
        List<UUID> couponIds = new ArrayList<>();

        for (int i = 0; i < couponCount; i++) {
            final int index = i;
            UUID couponId = transactionTemplate.execute(status -> {
                Coupon coupon = Coupon.create(
                        "MULTI-" + System.currentTimeMillis() + "-" + index,
                        "동시성 다중 테스트 쿠폰",
                        DiscountType.FIXED,
                        5000L,
                        10000L,
                        LocalDateTime.now().minusDays(1),
                        LocalDateTime.now().plusDays(30),
                        testUserId
                );
                return couponRepository.save(coupon).getId();
            });
            couponIds.add(couponId);
            
            try { Thread.sleep(1); } catch (InterruptedException e) {}
        }

        ExecutorService executorService = Executors.newFixedThreadPool(couponCount);
        CountDownLatch latch = new CountDownLatch(couponCount);
        AtomicInteger successCount = new AtomicInteger(0);

        
        for (UUID couponId : couponIds) {
            executorService.submit(() -> {
                try {
                    couponService.reserveCoupon(
                            reserveRequest(testUserId, UUID.randomUUID(), 50000L),
                            couponId
                    );
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        
        assertThat(successCount.get()).isEqualTo(couponCount);

        
        couponIds.forEach(couponId -> {
            Coupon coupon = couponRepository.findById(couponId).orElseThrow();
            assertThat(coupon.getStatus()).isEqualTo(org.sparta.coupon.domain.enums.CouponStatus.RESERVED);
        });
    }

    */
/**
     * 【상태 기반 락 검증 테스트】
     *
     * 기능: 이미 예약된 쿠폰에 대한 추가 예약 시도는 모두 실패
     *
     * 시나리오:
     * 1. 첫 번째 예약 성공 (AVAILABLE → RESERVED)
     * 2. 이후 5개 스레드가 동시에 같은 쿠폰 예약 시도
     * 3. 각 스레드가 락을 획득하더라도 쿠폰 상태가 RESERVED이므로 비즈니스 예외 발생
     * 4. 모든 후속 요청 실패
     *
     * 성공 흐름:
     * - 실패 카운트: 5개 (모두 실패)
     * - DB 예약 건수: 1건 (처음 성공한 것만)
     *
     * 검증 포인트:
     * ✓ 분산락 + 도메인 상태 체크 이중 방어
     * ✓ 락 획득 후에도 도메인 규칙 검증 필수
     *//*

    @Test
    @DisplayName("예약 성공 후 다른 스레드의 예약 시도는 실패한다")
    void reserveCoupon_AfterSuccessfulReservation_SubsequentAttemptsFail() throws InterruptedException {
        
        UUID firstOrderId = UUID.randomUUID();

        couponService.reserveCoupon(reserveRequest(testUserId, firstOrderId, 50000L), testCouponId);
        
        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    couponService.reserveCoupon(
                            reserveRequest(testUserId, UUID.randomUUID(), 50000L),
                            testCouponId
                    );
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        assertThat(failureCount.get()).isEqualTo(threadCount);

        long reservationCount = couponReservationRepository.count();
        assertThat(reservationCount).isEqualTo(1);
    }

    private CouponRequest.Reserve reserveRequest(UUID userId, UUID orderId, long orderAmount) {
        return new CouponRequest.Reserve(userId, orderId, orderAmount);
    }
}
*/

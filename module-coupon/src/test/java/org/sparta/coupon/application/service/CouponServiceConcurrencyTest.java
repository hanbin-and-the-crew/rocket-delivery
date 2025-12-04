package org.sparta.coupon.application.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.coupon.domain.entity.Coupon;
import org.sparta.coupon.domain.enums.DiscountType;
import org.sparta.coupon.domain.repository.CouponRepository;
import org.sparta.coupon.domain.repository.CouponReservationRepository;
import org.sparta.coupon.presentation.CouponRequest;
import org.sparta.redis.util.LockAcquisitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@Testcontainers
@DisplayName("CouponService 분산 락 동시성 테스트")
class CouponServiceConcurrencyTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:6.2.11")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

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

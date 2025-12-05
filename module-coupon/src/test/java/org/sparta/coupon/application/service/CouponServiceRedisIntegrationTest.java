package org.sparta.coupon.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.coupon.TestContainersConfig;
import org.sparta.coupon.application.dto.CouponServiceResult;
import org.sparta.coupon.domain.entity.Coupon;
import org.sparta.coupon.domain.enums.DiscountType;
import org.sparta.coupon.domain.repository.CouponRepository;
import org.sparta.coupon.domain.repository.CouponReservationRepository;
import org.sparta.coupon.infrastructure.redis.CouponReservationCacheInfo;
import org.sparta.coupon.infrastructure.redis.CouponReservationRedisManager;
import org.sparta.coupon.presentation.CouponRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


@Disabled("Redis Testcontainers 필요 - CI 환경에서 Docker 설정 후 활성화")
@Transactional
@DisplayName("CouponService Redis 통합 테스트")
class CouponServiceRedisIntegrationTest extends TestContainersConfig {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponReservationRepository couponReservationRepository;

    @Autowired
    private CouponReservationRedisManager redisManager;

    private UUID testUserId;
    private UUID testCouponId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();

        
        Coupon coupon = Coupon.create(
                "REDIS-TEST",
                "Redis 테스트 쿠폰",
                DiscountType.FIXED,
                5000L,
                10000L,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30),
                testUserId
        );

        testCouponId = couponRepository.save(coupon).getId();
    }

    @Test
    @DisplayName("쿠폰 예약 시 Redis에 예약 정보가 저장된다")
    void reserveCoupon_SavesReservationToRedis() {
        
        UUID orderId = UUID.randomUUID();

        CouponServiceResult.Reserve result = couponService.reserveCoupon(
                reserveRequest(testUserId, orderId, 50000L),
                testCouponId
        );

        CouponReservationCacheInfo redisData =
                redisManager.getReservation(result.reservationId());

        assertThat(redisData).isNotNull();
        assertThat(redisData.reservationId()).isEqualTo(result.reservationId());
        assertThat(redisData.couponId()).isEqualTo(testCouponId);
        assertThat(redisData.orderId()).isEqualTo(orderId);
        assertThat(redisData.userId()).isEqualTo(testUserId);
    }

    @Test
    @DisplayName("쿠폰 사용 확정 시 Redis에서 예약 정보가 삭제된다")
    void confirmCoupon_DeletesReservationFromRedis() {
        
        UUID orderId = UUID.randomUUID();
        CouponServiceResult.Reserve reserveResult = couponService.reserveCoupon(
                reserveRequest(testUserId, orderId, 50000L),
                testCouponId
        );

        UUID reservationId = reserveResult.reservationId();

        assertThat(redisManager.getReservation(reservationId)).isNotNull();

        couponService.confirmCoupon(reservationId, orderId);

        CouponReservationCacheInfo redisData =
                redisManager.getReservation(reservationId);

        assertThat(redisData).isNull();
    }

    @Test
    @DisplayName("쿠폰 예약 취소 시 Redis에서 예약 정보가 삭제된다")
    void cancelReservation_DeletesReservationFromRedis() {
        
        UUID orderId = UUID.randomUUID();
        CouponServiceResult.Reserve reserveResult = couponService.reserveCoupon(
                reserveRequest(testUserId, orderId, 50000L),
                testCouponId
        );

        UUID reservationId = reserveResult.reservationId();

        
        assertThat(redisManager.getReservation(reservationId)).isNotNull();

        
        couponService.cancelReservation(reservationId);

        
        CouponReservationCacheInfo redisData =
                redisManager.getReservation(reservationId);

        assertThat(redisData).isNull();
    }

    @Test
    @DisplayName("여러 예약을 생성하면 각각 Redis에 독립적으로 저장된다")
    void reserveCoupon_MultipleReservations_StoredIndependently() {
        
        int reservationCount = 3;
        UUID[] couponIds = new UUID[reservationCount];
        UUID[] reservationIds = new UUID[reservationCount];

        
        for (int i = 0; i < reservationCount; i++) {
            Coupon coupon = Coupon.create(
                    "REDIS-MULTI-TEST-" + i,
                    "Redis 다중 테스트 쿠폰 " + i,
                    DiscountType.FIXED,
                    5000L,
                    10000L,
                    LocalDateTime.now().minusDays(1),
                    LocalDateTime.now().plusDays(30),
                    testUserId
            );
            couponIds[i] = couponRepository.save(coupon).getId();
        }
        
        for (int i = 0; i < reservationCount; i++) {
            CouponServiceResult.Reserve result = couponService.reserveCoupon(
                    reserveRequest(testUserId, UUID.randomUUID(), 50000L),
                    couponIds[i]
            );
            reservationIds[i] = result.reservationId();
        }

        for (int i = 0; i < reservationCount; i++) {
            CouponReservationCacheInfo redisData =
                    redisManager.getReservation(reservationIds[i]);

            assertThat(redisData).isNotNull();
            assertThat(redisData.couponId()).isEqualTo(couponIds[i]);
        }
    }

    @Test
    @DisplayName("Redis에 없는 예약 ID 조회 시 null을 반환한다")
    void getReservation_NonExistentId_ReturnsNull() {
        
        UUID nonExistentReservationId = UUID.randomUUID();

        CouponReservationCacheInfo redisData =
                redisManager.getReservation(nonExistentReservationId);

        assertThat(redisData).isNull();
    }

    @Test
    @DisplayName("예약 후 DB와 Redis 모두에 데이터가 존재한다")
    void reserveCoupon_DataExistsInBothDbAndRedis() {
        
        UUID orderId = UUID.randomUUID();

        CouponServiceResult.Reserve result = couponService.reserveCoupon(
                reserveRequest(testUserId, orderId, 50000L),
                testCouponId
        );

        UUID reservationId = result.reservationId();

        boolean existsInDb = couponReservationRepository.existsById(reservationId);
        assertThat(existsInDb).isTrue();

        CouponReservationCacheInfo redisData =
                redisManager.getReservation(reservationId);
        assertThat(redisData).isNotNull();

        assertThat(redisData.reservationId()).isEqualTo(reservationId);
        assertThat(redisData.couponId()).isEqualTo(testCouponId);
        assertThat(redisData.orderId()).isEqualTo(orderId);
        assertThat(redisData.userId()).isEqualTo(testUserId);
    }

    @Test
    @DisplayName("확정 후 DB와 Redis 모두에서 예약 정보가 삭제된다")
    void confirmCoupon_DeletesFromBothDbAndRedis() {
        
        UUID orderId = UUID.randomUUID();
        CouponServiceResult.Reserve reserveResult = couponService.reserveCoupon(
                reserveRequest(testUserId, orderId, 50000L),
                testCouponId
        );

        UUID reservationId = reserveResult.reservationId();
        
        couponService.confirmCoupon(reservationId, orderId);
        
        boolean existsInDb = couponReservationRepository.existsById(reservationId);
        assertThat(existsInDb).isFalse();
        
        CouponReservationCacheInfo redisData =
                redisManager.getReservation(reservationId);
        assertThat(redisData).isNull();
    }

    private CouponRequest.Reserve reserveRequest(UUID userId, UUID orderId, long orderAmount) {
        return new CouponRequest.Reserve(userId, orderId, orderAmount);
    }
}

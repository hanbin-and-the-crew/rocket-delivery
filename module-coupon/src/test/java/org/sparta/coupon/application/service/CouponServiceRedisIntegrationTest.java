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

    /**
     * 【Redis 캐싱 검증 - 예약 정보 저장】
     *
     * 기능: 쿠폰 예약 시 DB와 함께 Redis에도 예약 정보 저장 (TTL 5분)
     *
     * 시나리오:
     * 1. 쿠폰 예약 요청
     * 2. DB에 예약 정보 저장
     * 3. Redis에 예약 캐시 저장 (키: coupon:reservation:{reservationId})
     * 4. Redis에서 저장된 데이터 조회 및 검증
     *
     * 성공 흐름:
     * - Redis에 예약 정보가 정상 저장됨
     * - reservationId, couponId, orderId, userId 모두 일치
     *
     * 검증 포인트:
     * ✓ Redis 캐싱 정상 동작
     * ✓ 예약 데이터의 정확성
     */
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

    /**
     * 【Redis 캐시 정리 검증 - 확정 시 삭제】
     *
     * 기능: 쿠폰 사용 확정 시 Redis 캐시 정리
     *
     * 시나리오:
     * 1. 쿠폰 예약 (Redis에 캐시 저장)
     * 2. Redis에 예약 정보 존재 확인
     * 3. 쿠폰 사용 확정 (RESERVED → PAID)
     * 4. Redis에서 예약 정보 삭제
     * 5. Redis 조회 시 null 반환 확인
     *
     * 성공 흐름:
     * - 확정 전: Redis에 데이터 존재
     * - 확정 후: Redis에서 데이터 삭제됨
     *
     * 검증 포인트:
     * ✓ 확정 후 불필요한 캐시 정리
     * ✓ DB와 Redis 동기화 보장
     */
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

    /**
     * 【Redis 캐시 정리 검증 - 취소 시 삭제】
     *
     * 기능: 쿠폰 예약 취소 시 Redis 캐시 정리
     *
     * 시나리오:
     * 1. 쿠폰 예약 (Redis에 캐시 저장)
     * 2. Redis에 예약 정보 존재 확인
     * 3. 쿠폰 예약 취소 (RESERVED → AVAILABLE)
     * 4. Redis에서 예약 정보 삭제
     * 5. Redis 조회 시 null 반환 확인
     *
     * 성공 흐름:
     * - 취소 전: Redis에 데이터 존재
     * - 취소 후: Redis에서 데이터 삭제됨
     *
     * 검증 포인트:
     * ✓ 취소 후 캐시 정리로 메모리 효율성 확보
     * ✓ DB와 Redis 동기화 보장
     */
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

    /**
     * 【Redis 다중 캐시 격리성 검증】
     *
     * 기능: 여러 쿠폰 예약이 각각 독립적인 Redis 키로 저장
     *
     * 시나리오:
     * 1. 3개의 서로 다른 쿠폰 생성
     * 2. 각 쿠폰에 대해 예약 생성
     * 3. 각 예약이 독립적인 Redis 키에 저장됨
     * 4. 모든 예약을 Redis에서 개별 조회하여 검증
     *
     * 성공 흐름:
     * - 3개의 예약이 모두 Redis에 존재
     * - 각 예약은 올바른 couponId를 가짐
     *
     * 검증 포인트:
     * ✓ Redis 키 격리성 - 서로 영향 없음
     * ✓ 대량 예약 처리 시 캐시 정합성
     */
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

    /**
     * 【Redis 캐시 미스 처리 검증】
     *
     * 기능: 존재하지 않는 예약 ID 조회 시 null 반환
     *
     * 시나리오:
     * 1. 존재하지 않는 UUID 생성
     * 2. Redis에서 해당 키로 조회
     * 3. null 반환 확인
     *
     * 성공 흐름:
     * - 예외 없이 null 정상 반환
     *
     * 검증 포인트:
     * ✓ 캐시 미스 시 안전한 처리
     * ✓ NPE 없이 null 반환
     */
    @Test
    @DisplayName("Redis에 없는 예약 ID 조회 시 null을 반환한다")
    void getReservation_NonExistentId_ReturnsNull() {
        
        UUID nonExistentReservationId = UUID.randomUUID();

        CouponReservationCacheInfo redisData =
                redisManager.getReservation(nonExistentReservationId);

        assertThat(redisData).isNull();
    }

    /**
     * 【DB-Redis 이중 저장 검증】
     *
     * 기능: 쿠폰 예약 시 DB와 Redis 양쪽에 모두 저장
     *
     * 시나리오:
     * 1. 쿠폰 예약 요청
     * 2. DB에 예약 정보 저장 확인 (existsById)
     * 3. Redis에 예약 캐시 저장 확인 (getReservation)
     * 4. 두 저장소의 데이터 일치성 검증
     *
     * 성공 흐름:
     * - DB 존재 여부: true
     * - Redis 존재 여부: not null
     * - 두 저장소의 데이터 완전 일치
     *
     * 검증 포인트:
     * ✓ Write-Through 캐시 패턴 정상 동작
     * ✓ DB와 Redis 데이터 정합성
     */
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

    /**
     * 【DB-Redis 이중 정리 검증】
     *
     * 기능: 쿠폰 확정 시 DB와 Redis 양쪽에서 모두 예약 정보 삭제
     *
     * 시나리오:
     * 1. 쿠폰 예약 (DB + Redis 저장)
     * 2. 쿠폰 사용 확정
     * 3. DB에서 예약 정보 삭제 확인 (existsById → false)
     * 4. Redis에서 캐시 삭제 확인 (getReservation → null)
     *
     * 성공 흐름:
     * - DB 존재 여부: false
     * - Redis 존재 여부: null
     *
     * 검증 포인트:
     * ✓ 확정 후 완전한 데이터 정리
     * ✓ DB와 Redis 동기화 삭제
     * ✓ 메모리 누수 방지
     */
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

package org.sparta.coupon.infrastructure.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * 쿠폰 예약 Redis 관리자
 * - 예약 정보를 Redis에 저장 (TTL 5분)
 * - TTL 만료 시 자동으로 데이터 삭제됨
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponReservationRedisManager {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String RESERVATION_KEY_PREFIX = "coupon:reservation:";
    private static final Duration RESERVATION_TTL = Duration.ofMinutes(5);

    /**
     * 예약 정보 저장
     */
    public void saveReservation(UUID reservationId, CouponReservationCacheInfo data) {
        String key = RESERVATION_KEY_PREFIX + reservationId;

        try {
            String jsonData = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(key, jsonData, RESERVATION_TTL);
            log.debug("예약 정보 Redis 저장: reservationId={}", reservationId);
        } catch (JsonProcessingException e) {
            log.error("예약 정보 JSON 직렬화 실패: reservationId={}", reservationId, e);
        }
    }

    /**
     * 예약 정보 조회
     */
    public CouponReservationCacheInfo getReservation(UUID reservationId) {
        String key = RESERVATION_KEY_PREFIX + reservationId;
        Object value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            return null;
        }

        try {
            return objectMapper.readValue(value.toString(), CouponReservationCacheInfo.class);
        } catch (JsonProcessingException e) {
            log.error("예약 정보 JSON 역직렬화 실패: reservationId={}", reservationId, e);
            return null;
        }
    }

    /**
     * 예약 정보 삭제
     */
    public void deleteReservation(UUID reservationId) {
        String key = RESERVATION_KEY_PREFIX + reservationId;
        redisTemplate.delete(key);
        log.debug("예약 정보 Redis 삭제: reservationId={}", reservationId);
    }

}

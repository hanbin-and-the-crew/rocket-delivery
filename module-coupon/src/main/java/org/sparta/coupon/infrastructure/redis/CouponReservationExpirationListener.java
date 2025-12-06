package org.sparta.coupon.infrastructure.redis;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.coupon.application.service.CouponReservationExpirationService;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Redis TTL 만료 이벤트를 구독하여 쿠폰 예약을 정리하는 리스너
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponReservationExpirationListener implements MessageListener {

    private static final String RESERVATION_KEY_PREFIX = "coupon:reservation:";

    private final RedisMessageListenerContainer listenerContainer;
    private final CouponReservationExpirationService expirationService;

    @PostConstruct
    public void subscribe() {
        listenerContainer.addMessageListener(this, new PatternTopic("__keyevent@*__:expired"));
        log.info("Redis 키 만료 이벤트 구독 시작");
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = new String(message.getBody(), StandardCharsets.UTF_8);

        if (!expiredKey.startsWith(RESERVATION_KEY_PREFIX)) {
            return;
        }

        String reservationIdValue = expiredKey.substring(RESERVATION_KEY_PREFIX.length());

        try {
            UUID reservationId = UUID.fromString(reservationIdValue);
            expirationService.handleExpiredReservation(reservationId);
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 예약 키 형식으로 인해 만료 처리를 건너뜁니다: key={}", expiredKey, e);
        }
    }
}

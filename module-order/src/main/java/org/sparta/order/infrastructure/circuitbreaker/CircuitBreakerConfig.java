package org.sparta.order.infrastructure.circuitbreaker;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Circuit Breaker 설정 클래스
 *
 * application.yml에서 설정값을 읽어옵니다.
 *
 * 예시:
 * circuit-breaker:
 *   failure-threshold: 5
 *   success-threshold: 3
 *   timeout-seconds: 60
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "circuit-breaker")
public class CircuitBreakerConfig {

    /**
     * CLOSED -> OPEN으로 전환되는 연속 실패 임계값
     * 기본값: 5회
     */
    private int failureThreshold = 5;

    /**
     * HALF_OPEN -> CLOSED로 전환되는 연속 성공 임계값
     * 기본값: 3회
     */
    private int successThreshold = 3;

    /**
     * OPEN 상태 유지 시간 (초 단위)
     * 이 시간이 지나면 HALF_OPEN으로 전환
     * 기본값: 60초
     */
    private long timeoutSeconds = 60;

    /**
     * OPEN 상태 유지 시간을 Duration으로 반환
     */
    public Duration getTimeout() {
        return Duration.ofSeconds(timeoutSeconds);
    }
}

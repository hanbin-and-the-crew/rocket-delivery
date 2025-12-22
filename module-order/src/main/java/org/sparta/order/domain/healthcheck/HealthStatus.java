package org.sparta.order.domain.healthcheck;

/**
 * 외부 서비스의 Health 상태
 */
public enum HealthStatus {
    UP,
    DOWN,
    DEGRADED
}
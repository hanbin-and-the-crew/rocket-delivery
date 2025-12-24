package org.sparta.order.domain.healthcheck;

/**
 * 외부 서비스의 Health 상태를 확인하는 인터페이스
 */
public interface HealthChecker {

    /**
     * 특정 서비스의 Health 상태 확인
     */
    HealthStatus check(String serviceName);

    /**
     * 모든 외부 서비스의 Health 상태를 주기적으로 확인
     */
    void checkAllServices();
}
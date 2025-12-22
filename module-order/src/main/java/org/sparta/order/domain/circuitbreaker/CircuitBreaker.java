package org.sparta.order.domain.circuitbreaker;

import java.util.function.Supplier;

/**
 * Circuit Breaker 인터페이스
 *
 * 외부 서비스 호출 시 장애 격리 및 Fail Fast 전략을 제공합니다.
 *
 * 주요 기능:
 * - 외부 서비스 호출을 감싸서 실패 카운트 추적
 * - 임계값 초과 시 Circuit을 OPEN하여 즉시 실패 응답
 * - 일정 시간 후 HALF_OPEN 상태로 전환하여 서비스 회복 확인
 */
public interface CircuitBreaker {

    /**
     * Circuit Breaker로 감싼 작업을 실행합니다.
     */
    <T> T execute(Supplier<T> operation, String serviceName) throws Exception;

    /**
     * 특정 서비스의 Circuit이 OPEN 상태인지 확인합니다.
     */
    boolean isOpen(String serviceName);

    /**
     * 특정 서비스의 현재 Circuit Breaker 상태를 반환합니다.
     */
    CircuitBreakerState getState(String serviceName);

    /**
     * Health Check 등 외부 감시 로직이 실패를 감지했을 때
     * Circuit Breaker에 수동으로 실패를 기록합니다.
     */
    void recordFailure(String serviceName);

    /**
     * 특정 서비스의 Circuit Breaker를 초기화합니다. (테스트 용도)
     */
    void reset(String serviceName);
}

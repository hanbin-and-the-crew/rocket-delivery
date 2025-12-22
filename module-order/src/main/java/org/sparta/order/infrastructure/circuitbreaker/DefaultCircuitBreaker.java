package org.sparta.order.infrastructure.circuitbreaker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.order.domain.circuitbreaker.CircuitBreaker;
import org.sparta.order.domain.circuitbreaker.CircuitBreakerMetrics;
import org.sparta.order.domain.circuitbreaker.CircuitBreakerOpenException;
import org.sparta.order.domain.circuitbreaker.CircuitBreakerState;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Circuit Breaker 기본 구현체
 *
 * Thread-safe하게 여러 서비스의 Circuit Breaker를 관리합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultCircuitBreaker implements CircuitBreaker {

    private final CircuitBreakerConfig config;
    private final Map<String, CircuitBreakerMetrics> metricsMap = new ConcurrentHashMap<>();

    @Override
    public <T> T execute(Supplier<T> operation, String serviceName) throws Exception {
        CircuitBreakerMetrics metrics = getOrCreateMetrics(serviceName);

        // 1. 현재 상태 확인 및 상태 전환 처리
        CircuitBreakerState currentState = updateState(metrics);

        // 2. OPEN 상태이면 즉시 실패
        if (currentState == CircuitBreakerState.OPEN) {
            log.warn("[Circuit Breaker] OPEN 상태 - 요청 차단: service={}", serviceName);
            throw new CircuitBreakerOpenException(serviceName);
        }

        // 3. 작업 실행 및 결과 처리
        try {
            T result = operation.get();
            onSuccess(metrics);
            return result;
        } catch (Exception e) {
            onFailure(metrics, e);
            throw e;
        }
    }

    @Override
    public boolean isOpen(String serviceName) {
        CircuitBreakerMetrics metrics = getOrCreateMetrics(serviceName);
        return updateState(metrics) == CircuitBreakerState.OPEN;
    }

    @Override
    public CircuitBreakerState getState(String serviceName) {
        CircuitBreakerMetrics metrics = getOrCreateMetrics(serviceName);
        return updateState(metrics);
    }

    @Override
    public void recordFailure(String serviceName) {
        CircuitBreakerMetrics metrics = getOrCreateMetrics(serviceName);
        updateState(metrics); // 최신 상태 반영
        handleFailure(metrics, "Health check failure");
    }

    @Override
    public void reset(String serviceName) {
        CircuitBreakerMetrics metrics = getOrCreateMetrics(serviceName);
        metrics.reset();
        log.info("[Circuit Breaker] 초기화됨: service={}", serviceName);
    }

    /**
     * 메트릭 조회 또는 생성
     */
    private CircuitBreakerMetrics getOrCreateMetrics(String serviceName) {
        return metricsMap.computeIfAbsent(serviceName, CircuitBreakerMetrics::new);
    }

    /**
     * 상태 업데이트 및 전환 처리
     */
    private CircuitBreakerState updateState(CircuitBreakerMetrics metrics) {
        CircuitBreakerState currentState = metrics.getState();

        switch (currentState) {
            case CLOSED:
                // CLOSED 상태에서는 실패 카운트만 확인
                if (metrics.getFailureCount() >= config.getFailureThreshold()) {
                    transitionToOpen(metrics);
                    return CircuitBreakerState.OPEN;
                }
                return CircuitBreakerState.CLOSED;

            case OPEN:
                // OPEN 상태에서 timeout 경과 시 HALF_OPEN으로 전환
                if (shouldTransitionToHalfOpen(metrics)) {
                    transitionToHalfOpen(metrics);
                    return CircuitBreakerState.HALF_OPEN;
                }
                return CircuitBreakerState.OPEN;

            case HALF_OPEN:
                // HALF_OPEN 상태는 그대로 유지 (성공/실패 시 onSuccess/onFailure에서 처리)
                return CircuitBreakerState.HALF_OPEN;

            default:
                return CircuitBreakerState.CLOSED;
        }
    }

    /**
     * HALF_OPEN으로 전환할 시점인지 확인
     */
    private boolean shouldTransitionToHalfOpen(CircuitBreakerMetrics metrics) {
        LocalDateTime stateChangedAt = metrics.getStateChangedAt();
        if (stateChangedAt == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(stateChangedAt.plus(config.getTimeout()));
    }

    /**
     * 성공 처리
     */
    private void onSuccess(CircuitBreakerMetrics metrics) {
        CircuitBreakerState state = metrics.getState();

        if (state == CircuitBreakerState.HALF_OPEN) {
            metrics.incrementSuccessCount();
            log.debug("[Circuit Breaker] HALF_OPEN 성공: service={}, successCount={}",
                metrics.getServiceName(), metrics.getSuccessCount());

            // HALF_OPEN에서 성공 임계값 도달 시 CLOSED로 전환
            if (metrics.getSuccessCount() >= config.getSuccessThreshold()) {
                transitionToClosed(metrics);
            }
        } else if (state == CircuitBreakerState.CLOSED) {
            // CLOSED 상태에서 성공 시 실패 카운트 초기화
            if (metrics.getFailureCount() > 0) {
                metrics.resetFailureCount();
                log.debug("[Circuit Breaker] CLOSED 상태 실패 카운트 초기화: service={}",
                    metrics.getServiceName());
            }
        }
    }

    /**
     * 실패 처리
     */
    private void onFailure(CircuitBreakerMetrics metrics, Exception exception) {
        handleFailure(metrics, exception.getMessage());
    }

    private void handleFailure(CircuitBreakerMetrics metrics, String reason) {
        CircuitBreakerState state = metrics.getState();

        metrics.incrementFailureCount();
        log.warn("[Circuit Breaker] 실패 감지: service={}, state={}, failureCount={}, exception={}",
            metrics.getServiceName(), state, metrics.getFailureCount(), reason);

        if (state == CircuitBreakerState.HALF_OPEN) {
            // HALF_OPEN에서 실패 시 즉시 OPEN으로 전환
            transitionToOpen(metrics);
        } else if (state == CircuitBreakerState.CLOSED) {
            // CLOSED에서 실패 임계값 도달 시 OPEN으로 전환
            if (metrics.getFailureCount() >= config.getFailureThreshold()) {
                transitionToOpen(metrics);
            }
        }
    }

    /**
     * OPEN 상태로 전환
     */
    private void transitionToOpen(CircuitBreakerMetrics metrics) {
        metrics.setState(CircuitBreakerState.OPEN);
        metrics.resetSuccessCount();
        log.error("[Circuit Breaker] 상태 전환: OPEN - service={}, failureCount={}",
            metrics.getServiceName(), metrics.getFailureCount());
    }

    /**
     * HALF_OPEN 상태로 전환
     */
    private void transitionToHalfOpen(CircuitBreakerMetrics metrics) {
        metrics.setState(CircuitBreakerState.HALF_OPEN);
        metrics.resetFailureCount();
        metrics.resetSuccessCount();
        log.info("[Circuit Breaker] 상태 전환: HALF_OPEN - service={}", metrics.getServiceName());
    }

    /**
     * CLOSED 상태로 전환
     */
    private void transitionToClosed(CircuitBreakerMetrics metrics) {
        metrics.setState(CircuitBreakerState.CLOSED);
        metrics.resetFailureCount();
        metrics.resetSuccessCount();
        log.info("[Circuit Breaker] 상태 전환: CLOSED - service={}", metrics.getServiceName());
    }
}

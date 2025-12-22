package org.sparta.order.domain.circuitbreaker;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Circuit Breaker의 메트릭을 관리하는 클래스
 * Thread-safe하게 실패/성공 카운트, 상태, 타이머 등을 관리합니다.
 */
public class CircuitBreakerMetrics {

    private final String serviceName;
    private final AtomicReference<CircuitBreakerState> state;
    private final AtomicInteger failureCount;
    private final AtomicInteger successCount;
    private final AtomicReference<LocalDateTime> lastFailureTime;
    private final AtomicReference<LocalDateTime> stateChangedAt;

    public CircuitBreakerMetrics(String serviceName) {
        this.serviceName = serviceName;
        this.state = new AtomicReference<>(CircuitBreakerState.CLOSED);
        this.failureCount = new AtomicInteger(0);
        this.successCount = new AtomicInteger(0);
        this.lastFailureTime = new AtomicReference<>();
        this.stateChangedAt = new AtomicReference<>(LocalDateTime.now());
    }

    public String getServiceName() {
        return serviceName;
    }

    public CircuitBreakerState getState() {
        return state.get();
    }

    public void setState(CircuitBreakerState newState) {
        CircuitBreakerState oldState = this.state.getAndSet(newState);
        if (oldState != newState) {
            this.stateChangedAt.set(LocalDateTime.now());
        }
    }

    public int getFailureCount() {
        return failureCount.get();
    }

    public int getSuccessCount() {
        return successCount.get();
    }

    public void incrementFailureCount() {
        this.failureCount.incrementAndGet();
        this.lastFailureTime.set(LocalDateTime.now());
    }

    public void incrementSuccessCount() {
        this.successCount.incrementAndGet();
    }

    public void resetFailureCount() {
        this.failureCount.set(0);
    }

    public void resetSuccessCount() {
        this.successCount.set(0);
    }

    public LocalDateTime getLastFailureTime() {
        return lastFailureTime.get();
    }

    public LocalDateTime getStateChangedAt() {
        return stateChangedAt.get();
    }

    public void reset() {
        this.state.set(CircuitBreakerState.CLOSED);
        this.failureCount.set(0);
        this.successCount.set(0);
        this.lastFailureTime.set(null);
        this.stateChangedAt.set(LocalDateTime.now());
    }

    @Override
    public String toString() {
        return String.format(
            "CircuitBreakerMetrics{serviceName='%s', state=%s, failureCount=%d, successCount=%d}",
            serviceName, state.get(), failureCount.get(), successCount.get()
        );
    }
}
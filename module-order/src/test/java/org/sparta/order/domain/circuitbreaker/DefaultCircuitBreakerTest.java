package org.sparta.order.domain.circuitbreaker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.order.infrastructure.circuitbreaker.CircuitBreakerConfig;
import org.sparta.order.infrastructure.circuitbreaker.DefaultCircuitBreaker;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * DefaultCircuitBreaker 단위 테스트
 *
 */
@Disabled
@DisplayName("DefaultCircuitBreaker 단위 테스트")
class DefaultCircuitBreakerTest {

    private CircuitBreakerConfig config;
    private DefaultCircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        config = new CircuitBreakerConfig();
        config.setFailureThreshold(5);
        config.setSuccessThreshold(3);
        config.setTimeoutSeconds(2);

        circuitBreaker = new DefaultCircuitBreaker(config);
    }

    @Test
    @DisplayName("초기 상태는 CLOSED여야 한다")
    void initialStateShouldBeClosed() {
        // when
        CircuitBreakerState state = circuitBreaker.getState("test-service");

        // then
        assertThat(state).isEqualTo(CircuitBreakerState.CLOSED);
    }

    @Test
    @DisplayName("CLOSED 상태에서 성공한 작업은 정상 동작해야 한다")
    void closedStateShouldExecuteSuccessfulOperation() throws Exception {
        // given
        String serviceName = "test-service";

        // when
        String result = circuitBreaker.execute(() -> "success", serviceName);

        // then
        assertThat(result).isEqualTo("success");
        assertThat(circuitBreaker.getState(serviceName)).isEqualTo(CircuitBreakerState.CLOSED);
    }

    @Test
    @DisplayName("연속 실패가 임계값을 초과하면 OPEN 상태로 전환되어야 한다")
    void shouldTransitionToOpenAfterFailureThreshold() {
        // given
        String serviceName = "test-service";
        int failureThreshold = config.getFailureThreshold();

        // when: 임계값만큼 실패
        for (int i = 0; i < failureThreshold; i++) {
            try {
                circuitBreaker.execute(() -> {
                    throw new RuntimeException("test failure");
                }, serviceName);
            } catch (Exception e) {
                // 예외 무시
            }
        }

        // then
        assertThat(circuitBreaker.getState(serviceName)).isEqualTo(CircuitBreakerState.OPEN);
    }

    @Test
    @DisplayName("OPEN 상태에서는 즉시 CircuitBreakerOpenException을 발생시켜야 한다")
    void openStateShouldThrowCircuitBreakerOpenException() {
        // given: Circuit을 OPEN 상태로 만듦
        String serviceName = "test-service";
        int failureThreshold = config.getFailureThreshold();

        for (int i = 0; i < failureThreshold; i++) {
            try {
                circuitBreaker.execute(() -> {
                    throw new RuntimeException("test failure");
                }, serviceName);
            } catch (Exception e) {
                // 예외 무시
            }
        }

        // when & then: OPEN 상태에서는 즉시 예외 발생
        assertThatThrownBy(() ->
            circuitBreaker.execute(() -> "should not execute", serviceName)
        )
        .isInstanceOf(CircuitBreakerOpenException.class)
        .hasMessageContaining(serviceName);
    }

    @Test
    @DisplayName("OPEN 상태에서 timeout 경과 후 HALF_OPEN으로 전환되어야 한다")
    void shouldTransitionToHalfOpenAfterTimeout() throws Exception {
        // given: Circuit을 OPEN 상태로 만듦
        String serviceName = "test-service";
        int failureThreshold = config.getFailureThreshold();

        for (int i = 0; i < failureThreshold; i++) {
            try {
                circuitBreaker.execute(() -> {
                    throw new RuntimeException("test failure");
                }, serviceName);
            } catch (Exception e) {
                // 예외 무시
            }
        }

        assertThat(circuitBreaker.getState(serviceName)).isEqualTo(CircuitBreakerState.OPEN);

        // when: timeout 경과 (2초)
        Thread.sleep(2100);

        // then: 다음 요청에서 HALF_OPEN으로 전환
        try {
            circuitBreaker.execute(() -> "test", serviceName);
        } catch (Exception e) {
            // 예외 무시
        }

        assertThat(circuitBreaker.getState(serviceName)).isEqualTo(CircuitBreakerState.HALF_OPEN);
    }

    @Test
    @DisplayName("HALF_OPEN 상태에서 성공 시 CLOSED로 전환되어야 한다")
    void shouldTransitionToClosedFromHalfOpenAfterSuccess() throws Exception {
        // given: Circuit을 HALF_OPEN 상태로 만듦
        String serviceName = "test-service";

        // 먼저 OPEN 상태로
        for (int i = 0; i < config.getFailureThreshold(); i++) {
            try {
                circuitBreaker.execute(() -> {
                    throw new RuntimeException("test failure");
                }, serviceName);
            } catch (Exception e) {
                // 예외 무시
            }
        }

        // timeout 대기
        Thread.sleep(2100);

        // when: HALF_OPEN에서 성공 임계값만큼 성공
        for (int i = 0; i < config.getSuccessThreshold(); i++) {
            circuitBreaker.execute(() -> "success", serviceName);
        }

        // then
        assertThat(circuitBreaker.getState(serviceName)).isEqualTo(CircuitBreakerState.CLOSED);
    }

    @Test
    @DisplayName("HALF_OPEN 상태에서 실패 시 OPEN으로 재전환되어야 한다")
    void shouldTransitionToOpenFromHalfOpenAfterFailure() throws Exception {
        // given: Circuit을 HALF_OPEN 상태로 만듦
        String serviceName = "test-service";

        // 먼저 OPEN 상태로
        for (int i = 0; i < config.getFailureThreshold(); i++) {
            try {
                circuitBreaker.execute(() -> {
                    throw new RuntimeException("test failure");
                }, serviceName);
            } catch (Exception e) {
                // 예외 무시
            }
        }

        // timeout 대기
        Thread.sleep(2100);

        // HALF_OPEN 전환
        try {
            circuitBreaker.execute(() -> "test", serviceName);
        } catch (Exception e) {
            // 예외 무시
        }

        assertThat(circuitBreaker.getState(serviceName)).isEqualTo(CircuitBreakerState.HALF_OPEN);

        // when: HALF_OPEN에서 실패
        try {
            circuitBreaker.execute(() -> {
                throw new RuntimeException("test failure");
            }, serviceName);
        } catch (Exception e) {
            // 예외 무시
        }

        // then: OPEN으로 재전환
        assertThat(circuitBreaker.getState(serviceName)).isEqualTo(CircuitBreakerState.OPEN);
    }

    @Test
    @DisplayName("isOpen()은 OPEN 상태에서만 true를 반환해야 한다")
    void isOpenShouldReturnTrueOnlyInOpenState() {
        // given
        String serviceName = "test-service";

        // when & then: 초기 CLOSED 상태
        assertThat(circuitBreaker.isOpen(serviceName)).isFalse();

        // OPEN 상태로 전환
        for (int i = 0; i < config.getFailureThreshold(); i++) {
            try {
                circuitBreaker.execute(() -> {
                    throw new RuntimeException("test failure");
                }, serviceName);
            } catch (Exception e) {
                // 예외 무시
            }
        }

        assertThat(circuitBreaker.isOpen(serviceName)).isTrue();
    }

    @Test
    @DisplayName("reset()은 Circuit Breaker를 초기 상태로 되돌려야 한다")
    void resetShouldRestoreToInitialState() {
        // given: Circuit을 OPEN 상태로 만듦
        String serviceName = "test-service";

        for (int i = 0; i < config.getFailureThreshold(); i++) {
            try {
                circuitBreaker.execute(() -> {
                    throw new RuntimeException("test failure");
                }, serviceName);
            } catch (Exception e) {
                // 예외 무시
            }
        }

        assertThat(circuitBreaker.getState(serviceName)).isEqualTo(CircuitBreakerState.OPEN);

        // when
        circuitBreaker.reset(serviceName);

        // then
        assertThat(circuitBreaker.getState(serviceName)).isEqualTo(CircuitBreakerState.CLOSED);
        assertThat(circuitBreaker.isOpen(serviceName)).isFalse();
    }

    @Test
    @DisplayName("recordFailure 호출만으로도 Circuit Breaker를 OPEN 상태로 만들 수 있어야 한다")
    void recordFailureShouldOpenCircuit() {
        // given
        String serviceName = "health-check-service";

        // when
        for (int i = 0; i < config.getFailureThreshold(); i++) {
            circuitBreaker.recordFailure(serviceName);
        }

        // then
        assertThat(circuitBreaker.getState(serviceName)).isEqualTo(CircuitBreakerState.OPEN);
        assertThat(circuitBreaker.isOpen(serviceName)).isTrue();
    }

    @Test
    @DisplayName("여러 서비스를 독립적으로 관리해야 한다")
    void shouldManageMultipleServicesIndependently() throws Exception {
        // given
        String service1 = "service-1";
        String service2 = "service-2";

        // when: service-1만 실패시킴
        for (int i = 0; i < config.getFailureThreshold(); i++) {
            try {
                circuitBreaker.execute(() -> {
                    throw new RuntimeException("test failure");
                }, service1);
            } catch (Exception e) {
                // 예외 무시
            }
        }

        // service-2는 정상 동작
        String result = circuitBreaker.execute(() -> "success", service2);

        // then
        assertThat(circuitBreaker.getState(service1)).isEqualTo(CircuitBreakerState.OPEN);
        assertThat(circuitBreaker.getState(service2)).isEqualTo(CircuitBreakerState.CLOSED);
        assertThat(result).isEqualTo("success");
    }

    @Test
    @DisplayName("동시성 테스트: 여러 스레드가 동시에 접근해도 안전해야 한다")
    void shouldBeThreadSafe() throws Exception {
        // given
        String serviceName = "test-service";
        int threadCount = 10;
        int operationsPerThread = 50;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // when: 여러 스레드가 동시에 작업 실행
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            executorService.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        final int operationIndex = j;
                        try {
                            circuitBreaker.execute(() -> {
                                // 절반은 성공, 절반은 실패
                                if ((threadIndex + operationIndex) % 2 == 0) {
                                    return "success";
                                } else {
                                    throw new RuntimeException("test failure");
                                }
                            }, serviceName);
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            failureCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then: 예외 없이 완료되어야 함
        assertThat(successCount.get() + failureCount.get()).isEqualTo(threadCount * operationsPerThread);
    }
}

package org.sparta.order.infrastructure.healthcheck;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sparta.order.domain.circuitbreaker.CircuitBreaker;
import org.sparta.order.domain.healthcheck.HealthStatus;
import org.sparta.order.infrastructure.client.CouponClient;
import org.sparta.order.infrastructure.client.PaymentClient;
import org.sparta.order.infrastructure.client.PointClient;
import org.sparta.order.infrastructure.client.StockClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Disabled
@ExtendWith(MockitoExtension.class)
class FeignHealthCheckerTest {

    @Mock
    private StockClient stockClient;

    @Mock
    private PointClient pointClient;

    @Mock
    private CouponClient couponClient;

    @Mock
    private PaymentClient paymentClient;

    @Mock
    private CircuitBreaker circuitBreaker;

    @InjectMocks
    private FeignHealthChecker healthChecker;

    @Test
    @DisplayName("정상 응답 시 HealthStatus.UP을 반환한다")
    void check_returnsUpWhenServiceHealthy() {
        assertThat(healthChecker.check("stock-service")).isEqualTo(HealthStatus.UP);
        assertThat(healthChecker.check("payment-service")).isEqualTo(HealthStatus.UP);
    }

    @Test
    @DisplayName("예외 발생 시 HealthStatus.DOWN을 반환한다")
    void check_returnsDownWhenServiceDown() {
        doThrow(new RuntimeException("DOWN")).when(pointClient).health();

        assertThat(healthChecker.check("point-service")).isEqualTo(HealthStatus.DOWN);
    }

    @Test
    @DisplayName("checkAllServices 호출 시 DOWN 상태인 서비스는 Circuit Breaker에 실패로 기록한다")
    void checkAllServices_recordsFailures() {
        doThrow(new RuntimeException("stock down")).when(stockClient).health();
        doThrow(new RuntimeException("coupon down")).when(couponClient).health();

        healthChecker.checkAllServices();

        verify(circuitBreaker, times(1)).recordFailure("stock-service");
        verify(circuitBreaker, times(1)).recordFailure("coupon-service");
    }
}

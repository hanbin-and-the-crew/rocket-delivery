package org.sparta.order.infrastructure.healthcheck;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.order.domain.circuitbreaker.CircuitBreaker;
import org.sparta.order.domain.healthcheck.HealthChecker;
import org.sparta.order.domain.healthcheck.HealthStatus;
import org.sparta.order.infrastructure.client.CouponClient;
import org.sparta.order.infrastructure.client.PaymentClient;
import org.sparta.order.infrastructure.client.PointClient;
import org.sparta.order.infrastructure.client.StockClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Feign Client를 이용한 Health Check 구현
 * - 주기적으로 외부 서비스 health endpoint 호출
 * - Circuit Breaker와 연동하여 서비스 상태 감지
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeignHealthChecker implements HealthChecker {

    private final StockClient stockClient;
    private final PointClient pointClient;
    private final CouponClient couponClient;
    private final PaymentClient paymentClient;
    private final CircuitBreaker circuitBreaker;

    /**
     * 특정 서비스의 Health 상태 확인
     */
    @Override
    public HealthStatus check(String serviceName) {
        try {
            switch (serviceName) {
                case "stock-service":
                    stockClient.health();
                    break;
                case "point-service":
                    pointClient.health();
                    break;
                case "coupon-service":
                    couponClient.health();
                    break;
                case "payment-service":
                    paymentClient.health();
                    break;
                default:
                    log.warn("[Health Check] 알 수 없는 서비스: {}", serviceName);
                    return HealthStatus.DOWN;
            }

            log.debug("[Health Check] {} - UP", serviceName);
            return HealthStatus.UP;

        } catch (Exception e) {
            log.warn("[Health Check] {} - DOWN: {}", serviceName, e.getMessage());
            return HealthStatus.DOWN;
        }
    }

    /**
     * 모든 외부 서비스의 Health 상태를 주기적으로 확인
     * - 30초마다 실행
     * - DOWN 감지 시 Circuit Breaker에 실패 기록
     */
    @Scheduled(fixedDelay = 30000) // 30초마다
    @Override
    public void checkAllServices() {
        log.debug("[Health Check] 전체 서비스 Health 체크 시작");

        checkService("stock-service");
        checkService("point-service");
        checkService("coupon-service");
        checkService("payment-service");

        log.debug("[Health Check] 전체 서비스 Health 체크 완료");
    }

    private void checkService(String serviceName) {
        HealthStatus status = check(serviceName);

        if (status == HealthStatus.DOWN) {
            // Circuit Breaker에 실패 기록
            try {
                circuitBreaker.recordFailure(serviceName);
                log.info("[Health Check] {} Circuit Breaker 실패 기록", serviceName);
            } catch (Exception e) {
                log.error("[Health Check] Circuit Breaker 실패 기록 실패: {}", serviceName, e);
            }
        }
    }
}
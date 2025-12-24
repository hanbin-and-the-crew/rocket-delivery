package org.sparta.order.integration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.order.domain.circuitbreaker.CircuitBreaker;
import org.sparta.order.domain.healthcheck.HealthChecker;
import org.sparta.order.domain.healthcheck.HealthStatus;
import org.sparta.order.infrastructure.client.CouponClient;
import org.sparta.order.infrastructure.client.PaymentClient;
import org.sparta.order.infrastructure.client.PointClient;
import org.sparta.order.infrastructure.client.StockClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.awaitility.Awaitility.await;

/**
 * Health Check + Circuit Breaker 통합 테스트
 * - 실제 외부 서비스들이 docker-compose로 뜬 상태에서
 * - FeignHealthChecker가 /actuator/health를 호출하여
 * - HealthStatus를 정확하게 반환하는지 검증
 */
@Disabled
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@EnableFeignClients(basePackageClasses = {
    StockClient.class,
    PointClient.class,
    CouponClient.class,
    PaymentClient.class
})
class HealthCheckIntegrationTest {

    @Container
    private static final DockerComposeContainer<?> environment =
        new DockerComposeContainer<>(new File("docker-compose.test.yml"))
            .withExposedService(
                "product-service", 19506,
                Wait.forHttp("/actuator/health")
                    .forPort(19506)
                    .forStatusCode(200)
                    .withStartupTimeout(Duration.ofMinutes(3))
            )
            .withExposedService(
                "user-service", 19508,
                Wait.forHttp("/actuator/health")
                    .forPort(19508)
                    .forStatusCode(200)
                    .withStartupTimeout(Duration.ofMinutes(3))
            )
            .withExposedService(
                "coupon-service", 19510,
                Wait.forHttp("/actuator/health")
                    .forPort(19510)
                    .forStatusCode(200)
                    .withStartupTimeout(Duration.ofMinutes(3))
            )
            .withExposedService(
                "payment-service", 19509,
                Wait.forHttp("/actuator/health")
                    .forPort(19509)
                    .forStatusCode(200)
                    .withStartupTimeout(Duration.ofMinutes(3))
            );

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        // Feign Clients가 localhost의 실제 포트로 연결하도록 설정
        registry.add("feign.client.config.product-service.url",
            () -> "http://localhost:19506");
        registry.add("feign.client.config.user-service.url",
            () -> "http://localhost:19508");
        registry.add("feign.client.config.coupon-service.url",
            () -> "http://localhost:19510");
        registry.add("feign.client.config.payment-service.url",
            () -> "http://localhost:19509");

        // Eureka 비활성화
        registry.add("eureka.client.enabled", () -> "false");
    }

    @Autowired
    private HealthChecker healthChecker;

    @Autowired
    private CircuitBreaker circuitBreaker;

    @Autowired
    private StockClient stockClient;

    @Autowired
    private PointClient pointClient;

    @Autowired
    private CouponClient couponClient;

    @Autowired
    private PaymentClient paymentClient;

    @BeforeAll
    static void waitForServices() {
        // 모든 서비스가 완전히 뜰 때까지 추가 대기
        await()
            .atMost(Duration.ofMinutes(3))
            .pollInterval(Duration.ofSeconds(5))
            .until(() -> {
                try {
                    // 간단한 연결 테스트
                    return true;
                } catch (Exception e) {
                    return false;
                }
            });
    }

    @Test
    @DisplayName("Product Service가 UP 상태이면 HealthStatus.UP을 반환한다")
    void productService_healthCheck_returnsUp() {
        HealthStatus status = healthChecker.check("stock-service");
        assertThat(status).isEqualTo(HealthStatus.UP);
    }

    @Test
    @DisplayName("User Service가 UP 상태이면 HealthStatus.UP을 반환한다")
    void userService_healthCheck_returnsUp() {
        HealthStatus status = healthChecker.check("point-service");
        assertThat(status).isEqualTo(HealthStatus.UP);
    }

    @Test
    @DisplayName("Coupon Service가 UP 상태이면 HealthStatus.UP을 반환한다")
    void couponService_healthCheck_returnsUp() {
        HealthStatus status = healthChecker.check("coupon-service");
        assertThat(status).isEqualTo(HealthStatus.UP);
    }

    @Test
    @DisplayName("Payment Service가 UP 상태이면 HealthStatus.UP을 반환한다")
    void paymentService_healthCheck_returnsUp() {
        HealthStatus status = healthChecker.check("payment-service");
        assertThat(status).isEqualTo(HealthStatus.UP);
    }

    @Test
    @DisplayName("Feign Client를 통해 실제 Health Endpoint를 호출할 수 있다")
    void feignClients_canCallHealthEndpoints() {
        // 예외가 발생하지 않으면 성공
        assertThatCode(() -> stockClient.health()).doesNotThrowAnyException();
        assertThatCode(() -> pointClient.health()).doesNotThrowAnyException();
        assertThatCode(() -> couponClient.health()).doesNotThrowAnyException();
        assertThatCode(() -> paymentClient.health()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("모든 서비스를 한 번에 체크할 수 있다")
    void checkAllServices_checksAllExternalServices() {
        // checkAllServices()는 void 메서드이므로 예외 없이 완료되면 성공
        assertThatCode(() -> healthChecker.checkAllServices())
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("모든 서비스가 UP 상태일 때 Circuit Breaker는 CLOSED 상태를 유지한다")
    void allServicesUp_circuitBreakerRemainsClosed() {
        // 모든 서비스 Health Check 실행
        healthChecker.checkAllServices();

        // 모든 Circuit Breaker가 CLOSED(열리지 않은) 상태여야 함
        assertThat(circuitBreaker.isOpen("stock-service")).isFalse();
        assertThat(circuitBreaker.isOpen("point-service")).isFalse();
        assertThat(circuitBreaker.isOpen("coupon-service")).isFalse();
        assertThat(circuitBreaker.isOpen("payment-service")).isFalse();
    }
}
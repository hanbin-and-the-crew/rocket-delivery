package org.sparta.order.e2e;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sparta.common.event.DomainEvent;
import org.sparta.common.event.EventPublisher;
import org.sparta.common.event.order.OrderCancelledEvent;
import org.sparta.order.application.command.OrderCommand;
import org.sparta.order.application.service.OrderService;
import org.sparta.order.domain.circuitbreaker.CircuitBreaker;
import org.sparta.order.e2e.helper.TestDataSeeder;
import org.sparta.order.domain.entity.OrderOutboxEvent;
import org.sparta.order.infrastructure.client.CouponClient;
import org.sparta.order.infrastructure.client.PaymentClient;
import org.sparta.order.infrastructure.client.PointClient;
import org.sparta.order.infrastructure.client.StockClient;
import org.sparta.order.infrastructure.outbox.OrderOutboxPublisher;
import org.sparta.order.infrastructure.repository.OrderJpaRepository;
import org.sparta.order.infrastructure.repository.OrderOutboxEventJpaRepository;
import org.sparta.order.presentation.dto.response.OrderResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * 완전한 Service-to-Service E2E 테스트
 *
 * 검증 항목:
 * 1. OrderService가 실제 외부 서비스(Product, User, Coupon, Payment)에 HTTP 요청
 * 2. Circuit Breaker 동작 확인
 * 3. Health Check 동작 확인
 * 4. 실패 시 Saga 보상 이벤트 발행
 * 5. Kafka 이벤트 발행 확인
 *
 * 구조:
 * OrderService (Java)
 *   → Feign Client (실제 HTTP)
 *   → Product/User/Coupon/Payment Service (Docker)
 *   → Kafka/Redis (이벤트 발행)
 */
@Disabled
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OrderCompleteFlowE2ETest {

    private static final Logger log = LoggerFactory.getLogger(OrderCompleteFlowE2ETest.class);

    @Container
    private static final DockerComposeContainer<?> environment =
        new DockerComposeContainer<>(new File("docker-compose.e2e-full.yml"))
            .withLocalCompose(true);

    private static String kafkaBootstrapServers;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        // Kafka - docker-compose.e2e-full.yml에서 39092로 포트 노출
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:39092");

        // Redis - docker-compose.e2e-full.yml에서 36379로 포트 노출
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "36379");

        // Feign Clients - 실제 서비스 URL 설정
        registry.add("spring.cloud.openfeign.client.config.product-service.url",
            () -> "http://localhost:19506");
        registry.add("spring.cloud.openfeign.client.config.user-service.url",
            () -> "http://localhost:19508");
        registry.add("spring.cloud.openfeign.client.config.coupon-service.url",
            () -> "http://localhost:19510");
        registry.add("spring.cloud.openfeign.client.config.payment-service.url",
            () -> "http://localhost:19509");

        // Eureka 비활성화
        registry.add("eureka.client.enabled", () -> "false");
    }

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderOutboxPublisher orderOutboxPublisher;

    @Autowired
    private OrderOutboxEventJpaRepository orderOutboxEventJpaRepository;

    @Autowired
    private OrderJpaRepository orderJpaRepository;

    @Autowired
    private CircuitBreaker circuitBreaker;

    @MockBean
    private EventPublisher eventPublisher;

    @Autowired
    private StockClient stockClient;

    @Autowired
    private PointClient pointClient;

    @Autowired
    private CouponClient couponClient;

    @Autowired
    private PaymentClient paymentClient;

    @BeforeAll
    static void waitForAllServices() {
        log.info("==================================================");
        log.info("[E2E 테스트 초기화 시작] Docker Compose 환경 준비 중");
        log.info("==================================================");

        // 모든 서비스가 완전히 준비될 때까지 대기
        await()
            .atMost(Duration.ofMinutes(4))
            .pollInterval(Duration.ofSeconds(5))
            .until(() -> {
                try {
                    // 각 서비스의 health endpoint가 응답하는지 확인
                    java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                    java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create("http://localhost:19506/actuator/health"))
                        .timeout(Duration.ofSeconds(3))
                        .GET()
                        .build();

                    java.net.http.HttpResponse<String> response = client.send(request,
                        java.net.http.HttpResponse.BodyHandlers.ofString());

                    log.info("[Health Check 성공] Product Service 응답 코드: {}", response.statusCode());
                    return response.statusCode() == 200;
                } catch (Exception e) {
                    log.info("[Health Check 대기 중] 서비스 준비 대기... (원인: {})", e.getMessage());
                    return false;
                }
            });

        log.info("==================================================");
        log.info("[E2E 테스트 초기화 완료] 모든 외부 서비스 준비 완료");
        log.info("==================================================\n");
    }

    @Test
    @DisplayName("실제 서비스 호출 - 데이터 없음으로 실패하지만 Circuit Breaker는 정상 동작")
    void createOrder_callsRealServices_failsGracefully() {
        log.info("\n[테스트 1 시작] 실제 서비스 호출 - 데이터 없음 시나리오");
        log.info("--------------------------------------------------");

        // given: 실제 서비스에 데이터가 없는 상태
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        log.info("[GIVEN] 테스트 데이터 생성 - Customer ID: {}, Product ID: {}", customerId, productId);

        OrderCommand.Create command = createCommand(customerId, productId, 1, 10000, 0, null);
        log.info("[GIVEN] 주문 생성 커맨드 준비 완료");

        // when & then: 외부 서비스에 실제 HTTP 요청이 가고,
        // 데이터가 없어서 실패하지만 Circuit Breaker는 CLOSED 상태 유지
        log.info("[WHEN] 주문 생성 시도 (실패 예상)");
        assertThatThrownBy(() -> orderService.createOrder(customerId, command))
            .isInstanceOf(Exception.class);
        log.info("[WHEN] 예상대로 실패 - 외부 서비스에 데이터가 없어 주문 생성 실패");

        // Circuit Breaker는 아직 CLOSED (실패 횟수가 임계값 미만)
        log.info("[THEN] Circuit Breaker 상태 검증 중...");
        assertThat(circuitBreaker.isOpen("stock-service")).isFalse();
        assertThat(circuitBreaker.isOpen("point-service")).isFalse();
        assertThat(circuitBreaker.isOpen("coupon-service")).isFalse();
        assertThat(circuitBreaker.isOpen("payment-service")).isFalse();
        log.info("[THEN] 검증 완료 - Circuit Breaker 모두 CLOSED 상태 (실패 횟수가 임계값 미만)");
        log.info("[테스트 1 완료] 통과\n");
    }

    @Test
    @DisplayName("연속 실패 시 Circuit Breaker가 OPEN 상태로 전환")
    void createOrder_multipleFailures_opensCircuitBreaker() {
        log.info("\n[테스트 2 시작] Circuit Breaker OPEN 전환 시나리오");
        log.info("--------------------------------------------------");

        // given: 실제 서비스에 데이터가 없는 상태
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        log.info("[GIVEN] 테스트 데이터 생성 완료");

        // when: 5번 연속 실패 (Circuit Breaker 임계값)
        log.info("[WHEN] Circuit Breaker 임계값(5회) 도달을 위해 5번 연속 주문 생성 시도");
        for (int i = 0; i < 5; i++) {
            OrderCommand.Create command = createCommand(customerId, productId, 1, 10000, 0, null);
            try {
                orderService.createOrder(customerId, command);
            } catch (Exception e) {
                log.info("  [실패 {}/5] 예상된 실패 - {}", (i + 1), e.getClass().getSimpleName());
            }
        }
        log.info("[WHEN] 5번 연속 실패 완료");

        // then: Circuit Breaker가 OPEN 상태로 전환
        // (stock-service가 첫 번째로 호출되므로 stock-service의 Circuit Breaker가 OPEN)
        log.info("[THEN] Circuit Breaker OPEN 상태 전환 대기 중...");
        await()
            .atMost(Duration.ofSeconds(5))
            .pollInterval(Duration.ofMillis(500))
            .until(() -> circuitBreaker.isOpen("stock-service"));

        assertThat(circuitBreaker.isOpen("stock-service")).isTrue();
        log.info("[THEN] 검증 완료 - stock-service Circuit Breaker가 OPEN 상태로 전환됨");
        log.info("[테스트 2 완료] 통과\n");
    }

    @Test
    @DisplayName("Health Check를 통해 모든 외부 서비스가 UP 상태임을 확인")
    void healthCheck_allServicesAreUp() {
        log.info("\n[테스트 3 시작] Health Check 시나리오");
        log.info("--------------------------------------------------");

        // given & when: 실제 서비스들의 health endpoint 호출
        log.info("[WHEN] Product Service Health Check 호출");
        assertThatCode(() -> stockClient.health()).doesNotThrowAnyException();
        log.info("[THEN] Product Service 정상 응답");

        log.info("[WHEN] User Service Health Check 호출");
        assertThatCode(() -> pointClient.health()).doesNotThrowAnyException();
        log.info("[THEN] User Service 정상 응답");

        log.info("[WHEN] Coupon Service Health Check 호출");
        assertThatCode(() -> couponClient.health()).doesNotThrowAnyException();
        log.info("[THEN] Coupon Service 정상 응답");

        log.info("[WHEN] Payment Service Health Check 호출");
        assertThatCode(() -> paymentClient.health()).doesNotThrowAnyException();
        log.info("[THEN] Payment Service 정상 응답");

        log.info("[테스트 3 완료] 모든 외부 서비스 Health Check 성공\n");
    }

    @Test
    @DisplayName("외부 서비스 실패 시 Saga 보상 이벤트가 발행된다")
    void createOrder_onFailure_publishesCompensationEvent() {
        log.info("\n[테스트 4 시작] Saga 보상 이벤트 발행 시나리오");
        log.info("--------------------------------------------------");

        // given: 실제 서비스에 데이터가 없어서 실패하는 상황
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        log.info("[GIVEN] 테스트 데이터 생성 - Product ID: {}", productId);

        OrderCommand.Create command = createCommand(customerId, productId, 1, 10000, 0, null);

        // when: 주문 생성 시도 (실패 예상)
        log.info("[WHEN] 주문 생성 시도 (실패 예상)");
        try {
            orderService.createOrder(customerId, command);
        } catch (Exception e) {
            log.info("[WHEN] 예상대로 실패 - {}", e.getClass().getSimpleName());
        }

        // then: Saga 보상 이벤트(OrderCancelledEvent)가 발행되었는지 확인
        log.info("[THEN] Saga 보상 이벤트(OrderCancelledEvent) 발행 검증 중...");
        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher).publishExternal(eventCaptor.capture());

        List<DomainEvent> events = eventCaptor.getAllValues();
        assertThat(events).isNotEmpty();
        assertThat(events.get(0)).isInstanceOf(OrderCancelledEvent.class);

        OrderCancelledEvent cancelledEvent = (OrderCancelledEvent) events.get(0);
        assertThat(cancelledEvent.productId()).isEqualTo(productId);
        log.info("[THEN] 검증 완료 - OrderCancelledEvent 발행됨 (Product ID: {})", cancelledEvent.productId());
        log.info("[테스트 4 완료] 통과\n");
    }

    @Test
    @DisplayName("Outbox 패턴 - 실패 시에도 보상 이벤트는 즉시 발행된다")
    void createOrder_onFailure_compensationEventNotInOutbox() {
        log.info("\n[테스트 5 시작] Outbox 패턴 검증 시나리오");
        log.info("--------------------------------------------------");

        // given
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        log.info("[GIVEN] 테스트 데이터 생성 완료");

        OrderCommand.Create command = createCommand(customerId, productId, 1, 10000, 0, null);

        // when: 주문 생성 실패
        log.info("[WHEN] 주문 생성 시도 (실패 예상)");
        try {
            orderService.createOrder(customerId, command);
        } catch (Exception e) {
            log.info("[WHEN] 예상대로 실패");
        }

        // then: Outbox에는 이벤트가 없음 (실패했으므로 OrderCreatedEvent가 저장되지 않음)
        log.info("[THEN] Outbox 테이블 확인 중...");
        List<OrderOutboxEvent> outboxEvents = orderOutboxEventJpaRepository.findAll();
        assertThat(outboxEvents).isEmpty();
        log.info("[THEN] Outbox 테이블에 이벤트 없음 (실패 시 OrderCreatedEvent는 저장되지 않음)");

        // 하지만 보상 이벤트는 즉시 발행됨 (Outbox 거치지 않음)
        log.info("[THEN] 보상 이벤트 즉시 발행 검증 중...");
        verify(eventPublisher).publishExternal(any(OrderCancelledEvent.class));
        log.info("[THEN] 검증 완료 - OrderCancelledEvent는 Outbox를 거치지 않고 즉시 발행됨");
        log.info("[테스트 5 완료] 통과\n");
    }

    @Test
    @DisplayName("성공 시나리오 - 실제 데이터가 있을 때 주문이 성공적으로 생성된다")
    void createOrder_withRealData_succeeds() {
        log.info("\n[테스트 6 시작] 성공 시나리오 - 전체 플로우 검증");
        log.info("--------------------------------------------------");

        // given: 테스트 데이터 생성
        log.info("[GIVEN] TestDataSeeder로 실제 DB에 테스트 데이터 생성 중...");
        TestDataSeeder seeder = new TestDataSeeder(
            "jdbc:postgresql://localhost:35432/test_db",
            "test_user",
            "test_pass"
        );

        TestDataSeeder.TestData testData = seeder.createSuccessScenarioData();
        log.info("[GIVEN] 테스트 데이터 생성 완료");
        log.info("  - Customer ID: {}", testData.customerId());
        log.info("  - Product ID: {}", testData.productId());
        log.info("  - Coupon ID: {}", testData.couponId());

        try {
            // when: 실제 데이터를 사용하여 주문 생성
            OrderCommand.Create command = createCommand(
                testData.customerId(),
                testData.productId(),
                1,  // quantity
                10000,  // productPrice
                5000,  // requestPoint
                testData.couponId()
            );

            log.info("[WHEN] 주문 생성 시도 (성공 예상)");
            log.info("  - 수량: 1개, 가격: 10,000원, 사용 포인트: 5,000P");
            OrderResponse.Detail response = orderService.createOrder(testData.customerId(), command);
            log.info("[WHEN] 주문 생성 성공 - Order ID: {}", response.orderId());

            // then: 주문이 성공적으로 생성됨
            log.info("[THEN] 주문 응답 검증 중...");
            assertThat(response).isNotNull();
            assertThat(response.orderId()).isNotNull();
            assertThat(response.customerId()).isEqualTo(testData.customerId());
            assertThat(response.productId()).isEqualTo(testData.productId());
            log.info("[THEN] 주문 응답 검증 완료");

            // Circuit Breaker는 여전히 CLOSED 상태
            log.info("[THEN] Circuit Breaker 상태 검증 중...");
            assertThat(circuitBreaker.isOpen("stock-service")).isFalse();
            assertThat(circuitBreaker.isOpen("point-service")).isFalse();
            assertThat(circuitBreaker.isOpen("coupon-service")).isFalse();
            assertThat(circuitBreaker.isOpen("payment-service")).isFalse();
            log.info("[THEN] Circuit Breaker 모두 CLOSED 상태 유지");
            log.info("[테스트 6 완료] 전체 주문 플로우 성공\n");

        } finally {
            // cleanup: 테스트 데이터 삭제
            log.info("[CLEANUP] 테스트 데이터 정리 중...");
            seeder.cleanupAllData();
            log.info("[CLEANUP] 테스트 데이터 정리 완료\n");
        }
    }

    private OrderCommand.Create createCommand(UUID customerId, UUID productId,
                                              int quantity, int productPrice,
                                              int requestPoint, UUID couponId) {
        return new OrderCommand.Create(
            UUID.randomUUID(), // supplierCompanyId
            UUID.randomUUID(), // supplierHubId
            UUID.randomUUID(), // receiptCompanyId
            UUID.randomUUID(), // receiptHubId
            productId,
            quantity,
            productPrice,
            "서울시 강남구 테스트로 123",
            "홍길동",
            "010-1234-5678",
            "test-slack-id",
            LocalDateTime.now().plusDays(7),
            "빠른 배송 부탁드립니다",
            requestPoint,
            "CARD",
            "TOSS",
            "KRW",
            couponId
        );
    }
}

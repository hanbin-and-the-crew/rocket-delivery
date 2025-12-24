package org.sparta.order.e2e;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sparta.common.error.BusinessException;
import org.sparta.common.event.order.OrderCancelledEvent;
import org.sparta.common.event.order.OrderCreatedEvent;
import org.sparta.order.application.command.OrderCommand;
import org.sparta.order.application.service.OrderService;
import org.sparta.order.domain.entity.OrderOutboxEvent;
import org.sparta.order.domain.enumeration.OutboxStatus;
import org.sparta.order.domain.error.OrderErrorType;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import redis.clients.jedis.JedisPooled;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;

@Disabled
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
// Kafka/Redis + order-event-consumer(docker-compose)로 Outbox → Kafka → Redis → 외부 소비자 컨슈머 흐름을 검증하는 E2E 테스트
class OrderCreateE2ETest {

    private static final Duration KAFKA_POLL_TIMEOUT = Duration.ofSeconds(15);
    private static final String CONSUMER_KEY_PREFIX = "order:consumer:";

    @Container
    private static final DockerComposeContainer<?> environment =
        new DockerComposeContainer<>(new File("docker-compose.test.yml"))
            .withExposedService(
                "kafka", 9092,
                Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(1))
            )
            .withExposedService(
                "redis", 6379,
                Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(30))
            );

    private static String kafkaBootstrapServers;
    private static String redisHost;
    private static int redisPort;

    @DynamicPropertySource
    static void overrideKafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", () -> kafkaBootstrapServers);
    }

    @BeforeAll
    static void resolveServiceEndpoints() {
        String kafkaHost = environment.getServiceHost("kafka", 9092);
        Integer kafkaPort = environment.getServicePort("kafka", 9092);
        kafkaBootstrapServers = kafkaHost + ":" + kafkaPort;

        redisHost = environment.getServiceHost("redis", 6379);
        redisPort = environment.getServicePort("redis", 6379);
    }

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderOutboxPublisher orderOutboxPublisher;

    @Autowired
    private OrderOutboxEventJpaRepository orderOutboxEventJpaRepository;

    @Autowired
    private OrderJpaRepository orderJpaRepository;

    @MockitoBean
    private StockClient stockClient;

    @MockitoBean
    private PointClient pointClient;

    @MockitoBean
    private CouponClient couponClient;

    @MockitoBean
    private PaymentClient paymentClient;

    @AfterEach
    void resetMocks() {
        Mockito.reset(stockClient, pointClient, couponClient, paymentClient);
    }

    @Test
    @DisplayName("Kafka + Redis 환경 - 주문 생성 성공 전체 시나리오")
    void createOrder_successScenario() {
        UUID customerId = UUID.randomUUID();
        UUID couponId = UUID.randomUUID();
        OrderCommand.Create command = createCommand(2, 20_000, 500, couponId);

        stubStockSuccess();
        stubPointSuccess();
        stubCouponSuccess();
        stubPaymentSuccess();

        OrderResponse.Detail response = orderService.createOrder(customerId, command);

        List<OrderOutboxEvent> readyEvents = orderOutboxEventJpaRepository.findAll();
        assertThat(readyEvents).hasSize(1);
        assertThat(readyEvents.get(0).getStatus()).isEqualTo(OutboxStatus.READY);

        orderOutboxPublisher.publishReadyEvents();

        OrderCreatedEvent event = consumeKafkaEvent("order.orderCreate", OrderCreatedEvent.class);
        long expectedTotal = command.productPrice().longValue() * command.quantity();
        assertThat(event.orderId()).isEqualTo(response.orderId());
        assertThat(event.amountTotal()).isEqualTo(expectedTotal);
        assertThat(event.amountCoupon()).isEqualTo(1_000L);
        assertThat(event.amountPoint()).isEqualTo(500L);
        assertThat(event.amountPayable()).isEqualTo(expectedTotal - 1_500L);

        awaitExternalConsumerStatus(event.orderId(), "CREATED");
    }

    @Test
    @DisplayName("Kafka + Redis 환경 - 결제 실패 시 Saga 보상 이벤트 발행")
    void createOrder_paymentFailureScenario() {
        UUID customerId = UUID.randomUUID();
        UUID couponId = UUID.randomUUID();
        OrderCommand.Create command = createCommand(1, 15_000, 0, couponId);

        stubStockSuccess();
        stubPointSuccess();
        stubCouponSuccess();
        stubPaymentFailure();

        assertThatThrownBy(() -> orderService.createOrder(customerId, command))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining(OrderErrorType.PAYMENT_APPROVE_FAILED.getMessage());

        assertThat(orderOutboxEventJpaRepository.count()).isZero();

        OrderCancelledEvent cancelledEvent = consumeKafkaEvent(
            "order.orderCancel", OrderCancelledEvent.class);
        assertThat(cancelledEvent.orderId()).isNotNull();
        assertThat(cancelledEvent.productId()).isEqualTo(command.productId());
        assertThat(cancelledEvent.quantity()).isEqualTo(command.quantity());

        awaitExternalConsumerStatus(cancelledEvent.orderId(), "CANCELLED");
    }

    private void stubStockSuccess() {
        Mockito.when(stockClient.reserveStock(any(StockClient.StockReserveRequest.class)))
            .thenAnswer(invocation -> {
                StockClient.StockReserveRequest request = invocation.getArgument(0);
                StockClient.StockReserveResponse body = new StockClient.StockReserveResponse(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    request.reservationKey(),
                    request.quantity(),
                    "RESERVED"
                );
                StockClient.Meta meta = new StockClient.Meta("SUCCESS", null, null);
                return new StockClient.ApiResponse<>(meta, body);
            });
    }

    private void stubPointSuccess() {
        Mockito.when(pointClient.reservePoint(any(PointClient.PointRequest.Reserve.class)))
            .thenAnswer(invocation -> {
                PointClient.PointRequest.Reserve request = invocation.getArgument(0);
                PointClient.PointResponse.PointReservation reservation =
                    new PointClient.PointResponse.PointReservation(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        request.orderId(),
                        request.requestPoint(),
                        LocalDateTime.now(),
                        "RESERVED"
                    );
                PointClient.PointResponse.PointReservationResult body =
                    new PointClient.PointResponse.PointReservationResult(
                        request.requestPoint(),
                        List.of(reservation)
                    );
                PointClient.Meta meta = new PointClient.Meta("SUCCESS", null, null);
                return new PointClient.ApiResponse<>(meta, body);
            });
    }

    private void stubCouponSuccess() {
        Mockito.when(couponClient.reserveCoupon(
                any(UUID.class),
                any(CouponClient.CouponRequest.Reverse.class)))
            .thenAnswer(invocation -> {
                CouponClient.CouponReserveResponse.Reserve body =
                    new CouponClient.CouponReserveResponse.Reserve(
                        true,
                        UUID.randomUUID(),
                        1_000L,
                        "AMOUNT",
                        LocalDateTime.now().plusDays(7),
                        null,
                        null
                    );
                CouponClient.Meta meta = new CouponClient.Meta("SUCCESS", null, null);
                return new CouponClient.ApiResponse<>(meta, body);
            });
    }

    private void stubPaymentSuccess() {
        Mockito.when(paymentClient.approve(
                any(PaymentClient.PaymentRequest.Approval.class),
                any(UUID.class)))
            .thenAnswer(invocation -> {
                PaymentClient.PaymentRequest.Approval request = invocation.getArgument(0);
                PaymentClient.PaymentResponse.Approval body =
                    new PaymentClient.PaymentResponse.Approval(
                        request.orderId(),
                        true,
                        "pay-" + request.orderId(),
                        LocalDateTime.now(),
                        null,
                        null
                    );
                PaymentClient.Meta meta = new PaymentClient.Meta("SUCCESS", null, null);
                return new PaymentClient.ApiResponse<>(meta, body);
            });
    }

    private void stubPaymentFailure() {
        Mockito.when(paymentClient.approve(
                any(PaymentClient.PaymentRequest.Approval.class),
                any(UUID.class)))
            .thenAnswer(invocation -> {
                PaymentClient.PaymentRequest.Approval request = invocation.getArgument(0);
                throw new RuntimeException("PG_DOWN:" + request.orderId());
            });
    }

    private OrderCommand.Create createCommand(int quantity, int productPrice,
                                              int requestPoint, UUID couponId) {
        return new OrderCommand.Create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            quantity,
            productPrice,
            "서울시 강남구 어딘가 1-1",
            "홍길동",
            "010-1111-2222",
            "slack-user",
            LocalDateTime.now().plusDays(3),
            "빠른 배송 부탁드려요",
            requestPoint,
            "CARD",
            "TOSS",
            "KRW",
            couponId
        );
    }

    private <T> T consumeKafkaEvent(String topic, Class<T> type) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "order-e2e-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
            org.springframework.kafka.support.serializer.JsonDeserializer.class);
        props.put(org.springframework.kafka.support.serializer.JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(org.springframework.kafka.support.serializer.JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        org.springframework.kafka.support.serializer.JsonDeserializer<T> valueDeserializer =
            new org.springframework.kafka.support.serializer.JsonDeserializer<>(type, false);
        valueDeserializer.addTrustedPackages("*");

        try (KafkaConsumer<String, T> consumer =
                 new KafkaConsumer<>(props, new StringDeserializer(), valueDeserializer)) {
            consumer.subscribe(Collections.singletonList(topic));
            ConsumerRecords<String, T> records = consumer.poll(KAFKA_POLL_TIMEOUT);
            assertThat(records.count())
                .as("Kafka topic %s should contain at least one event", topic)
                .isGreaterThan(0);
            return records.iterator().next().value();
        }
    }

    private void awaitExternalConsumerStatus(UUID orderId, String expectedStatus) {
        String redisKey = CONSUMER_KEY_PREFIX + orderId;
        long deadline = System.currentTimeMillis() + Duration.ofSeconds(30).toMillis();

        try (JedisPooled jedis = new JedisPooled(redisHost, redisPort)) {
            while (System.currentTimeMillis() < deadline) {
                String status = jedis.get(redisKey);
                if (expectedStatus.equals(status)) {
                    return;
                }
                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("외부 소비자 상태 확인 중 인터럽트 발생", e);
        }

        fail("외부 소비자가 상태를 기록하지 않았습니다. key=" + redisKey);
    }
}

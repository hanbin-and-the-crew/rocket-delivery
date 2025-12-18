package org.sparta.payment.infrastructure.kafka;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.*;
import org.sparta.common.event.payment.PaymentFailedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EmbeddedKafka(
        partitions = 1,
        topics = {
                "order.orderCreate",
                "order.orderCreate.DLT"
        }
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PaymentKafkaDLTTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaListenerEndpointRegistry registry;

    private Consumer<String, String> dltConsumer;

    @BeforeAll
    void setUp() {
        Map<String, Object> consumerProps =
                KafkaTestUtils.consumerProps(
                        "dlt-test-group",
                        "false",
                        embeddedKafkaBroker
                );

        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        dltConsumer = new KafkaConsumer<>(consumerProps);
        dltConsumer.subscribe(List.of("order.orderCreate.DLT"));
    }

    @BeforeEach
    void stopDltListener() {
        MessageListenerContainer container =
                registry.getListenerContainer("payment-dlt-listener");

        if (container != null) {
            container.stop();
        }
    }

    @AfterAll
    void tearDown() {
        dltConsumer.close();
    }

    @Test
    @DisplayName("시스템예외_발생시_DLT로_전송된다")
    void SystemErrorOccurThenSendToDLT() {
        // given: 실패를 유도하는 이벤트
        PaymentFailedEvent event = new PaymentFailedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1000L,
                "KRW",
                "SYSTEM_ERROR",
                "강제 실패 테스트"
        );

        // when: 원본 토픽으로 전송
        kafkaTemplate.send("order.orderCreate", event);

        // then: DLT에서 메시지 수신
        ConsumerRecords<String, String> records =
                KafkaTestUtils.getRecords(dltConsumer, Duration.ofSeconds(10));

        assertThat(records.count()).isGreaterThan(0);

        ConsumerRecord<String, String> record =
                records.iterator().next();

        assertThat(record.topic()).isEqualTo("order.orderCreate.DLT");
        assertThat(record.value()).contains("SYSTEM_ERROR");
    }
}

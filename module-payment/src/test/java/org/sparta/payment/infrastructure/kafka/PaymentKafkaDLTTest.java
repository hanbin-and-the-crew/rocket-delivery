//package org.sparta.payment.infrastructure.kafka;
//
//import org.apache.kafka.clients.consumer.*;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mockito;
//import org.sparta.common.event.order.OrderCreatedEvent;
//import org.sparta.payment.application.service.PaymentService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.kafka.test.EmbeddedKafkaBroker;
//import org.springframework.kafka.test.context.EmbeddedKafka;
//import org.springframework.kafka.test.utils.KafkaTestUtils;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.testcontainers.shaded.com.fasterxml.jackson.databind.deser.std.StringDeserializer;
//
//import java.time.Duration;
//import java.util.List;
//import java.util.Map;
//import java.util.UUID;
//
//import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
//import static org.mockito.ArgumentMatchers.any;
//
//@SpringBootTest
//@EmbeddedKafka(
//        partitions = 1,
//        topics = {
//                "order.orderCreate",
//                "order.orderCreate.DLT"
//        }
//)
//@ActiveProfiles("test")
//class PaymentKafkaDLTTest {
//
//    @Autowired
//    KafkaTemplate<String, Object> kafkaTemplate;
//
//    @Autowired
//    EmbeddedKafkaBroker embeddedKafkaBroker;
//
//    @MockitoBean
//    PaymentService paymentService;
//
//    Consumer<String, String> dltConsumer;
//
//    @BeforeEach
//    void setup() {
//        Mockito.doThrow(new RuntimeException("SYSTEM ERROR"))
//                .when(paymentService)
//                .storeCompletedPayment(any(), any());
//
//        Map<String, Object> props =
//                KafkaTestUtils.consumerProps("dlt-test", "false", embeddedKafkaBroker);
//
//        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
//        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
//        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
//
//        dltConsumer = new KafkaConsumer<>(props);
//        dltConsumer.subscribe(List.of("order.orderCreate.DLT"));
//    }
//
//    @Test
//    @DisplayName("시스템예외면_DLT로_간다")
//    void SystemErrorOccursThenSendToDLT() {
//        OrderCreatedEvent event = OrderCreatedEvent.of(
//                UUID.randomUUID(),
//                1000L,
//                0L,
//                0L,
//                1000L,
//                "CARD",
//                "KAKAO",
//                "SYSTEM_ERROR", // 여기로 실패 유도
//                null,
//                null,
//                "test-key"
//        );
//
//        kafkaTemplate.send("order.orderCreate", event);
//
//        ConsumerRecords<String, String> records =
//                KafkaTestUtils.getRecords(dltConsumer, Duration.ofSeconds(10));
//
//        assertThat(records.count()).isGreaterThan(0);
//
//        ConsumerRecord<String, String> record = records.iterator().next();
//        assertThat(record.topic()).isEqualTo("order.orderCreate.DLT");
//    }
//}
//package org.sparta.payment.infrastructure.kafka;
//
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.sparta.payment.infrastructure.consumer.PaymentDeadLetterEventHandler;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.kafka.test.context.EmbeddedKafka;
//import org.springframework.test.context.TestPropertySource;
//import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
//
//import static org.awaitility.Awaitility.await;
//import static org.mockito.ArgumentMatchers.contains;
//import static org.mockito.Mockito.verify;
//
//import java.util.concurrent.TimeUnit;
//
//@Slf4j
//@SpringBootTest
//@EmbeddedKafka(
//        partitions = 1,
//        topics = {"order.orderCreate", "order.orderCreate.DLT"}
//)
//@TestPropertySource(properties = {
//        "spring.kafka.consumer.auto-offset-reset=earliest"
//})
//@DisplayName("Payment Kafka DLT 통합 테스트")
//class PaymentKafkaDLTIntegrationTest {
//
//    @Autowired
//    private KafkaTemplate<String, String> kafkaTemplate;
//
//    @MockitoSpyBean
//    private PaymentDeadLetterEventHandler dltHandler;
//
//    /**
//     * 1. 잘못된 JSON → 재시도 3회 후 DLT로 전송
//     */
//    @Test
//    @DisplayName("1. 잘못된 JSON 메시지 → DLT로 전송")
//    void testInvalidJsonMessageToDLT() {
//        // Given: 잘못된 JSON (닫는 괄호 없음)
//        String invalidJson = "{\"orderId\": \"order-123\"";
//
//        // When: 메시지 발송
//        kafkaTemplate.send("order.orderCreate", invalidJson);
//
//        // Then: DLT 핸들러 호출 확인
//        // 재시도 간격 1초 × 3회 + 처리시간 = 최대 6초 + 여유
//        await()
//                .atMost(8, TimeUnit.SECONDS)
//                .pollInterval(200, TimeUnit.MILLISECONDS)
//                .untilAsserted(() -> {
//                    verify(dltHandler).handleOrderCreateDLT(contains("orderId"));
//                    log.info("✓ 테스트 1 통과: 잘못된 메시지가 DLT로 전송됨");
//                });
//    }
//
//    /**
//     * 2. RuntimeException 발생 → 재시도 후 DLT로 전송
//     */
//    @Test
//    @DisplayName("2. RuntimeException 발생 → DLT로 전송")
//    void testRuntimeExceptionToDLT() {
//        // Given: 예외 발생 트리거 메시지
//        String exceptionMessage = "null-order-id";
//
//        // When: 메시지 발송
//        kafkaTemplate.send("order.orderCreate", exceptionMessage);
//
//        // Then: DLT 핸들러 호출 확인
//        await()
//                .atMost(8, TimeUnit.SECONDS)
//                .pollInterval(200, TimeUnit.MILLISECONDS)
//                .untilAsserted(() -> {
//                    verify(dltHandler).handleOrderCreateDLT(contains("null-order-id"));
//                    log.info("✓ 테스트 2 통과: RuntimeException 메시지가 DLT로 전송됨");
//                });
//    }
//
//    /**
//     * 3. 정상 메시지 → DLT로 가지 않음
//     */
//    @Test
//    @DisplayName("3. 정상 메시지 → DLT 미전송")
//    void testValidMessageNotSentToDLT() {
//        // Given: 정상 JSON 메시지
//        String validMessage = "{\"orderId\": \"order-123\", \"amount\": 10000}";
//
//        // When: 메시지 발송
//        kafkaTemplate.send("order.orderCreate", validMessage);
//
//        // Then: DLT 핸들러가 호출되지 않음 (정상 처리)
//        await()
//                .atMost(3, TimeUnit.SECONDS)
//                .pollDelay(2, TimeUnit.SECONDS) // 충분한 처리 시간
//                .untilAsserted(() -> {
//                    // 이 메시지로는 DLT 핸들러가 호출되지 않아야 함
//                    verify(dltHandler, org.mockito.Mockito.never())
//                            .handleOrderCreateDLT(validMessage);
//                    log.info("✓ 테스트 3 통과: 정상 메시지는 DLT로 가지 않음");
//                });
//    }
//
//    /**
//     * 4. 연속된 메시지 발송 - 실패 메시지가 DLT로 전송되는지 확인
//     */
//    @Test
//    @DisplayName("4. 연속 메시지 발송 중 실패 메시지는 DLT로 전송")
//    void testMultipleMessagesWithPartialFailure() {
//        // Given: 실패 메시지
//        String invalidMsg = "{broken";
//
//        // When: 메시지 발송
//        kafkaTemplate.send("order.orderCreate", "key-fail", invalidMsg);
//
//        // Then: 실패한 메시지가 DLT로 전송됨
//        await()
//                .atMost(8, TimeUnit.SECONDS)
//                .pollInterval(200, TimeUnit.MILLISECONDS)
//                .untilAsserted(() -> {
//                    verify(dltHandler).handleOrderCreateDLT(contains("broken"));
//                    log.info("✓ 테스트 4 통과: 실패한 메시지가 DLT로 전송됨");
//                });
//    }
//}
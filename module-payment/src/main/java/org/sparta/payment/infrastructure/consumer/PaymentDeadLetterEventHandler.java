package org.sparta.payment.infrastructure.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentDeadLetterEventHandler {

    @KafkaListener(
            id = "payment-dlt-listener", // Test에서는 따로 소모하도록
            topics = "order.orderCreate.DLT",
            groupId = "payment-dlt-handler",
            containerFactory = "paymentKafkaListenerContainerFactory"
    )
    public void handleOrderCreateDLT(String message) {
        log.error("[DLT][ORDER_CREATE] topic = order.orderCreate 처리 실패 메시지 수신: {}", message);

        // TODO: DLT 처리는 사용자에 맞게 처리. 지금은 로그 처리만
        // 1. Slack or Discord 알림
        // 2. 장애 DB 적재
        // 3. 수동 재처리 API 연계
    }
}
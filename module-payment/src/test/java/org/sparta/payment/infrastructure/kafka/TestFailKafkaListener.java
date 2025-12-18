package org.sparta.payment.infrastructure.kafka;

import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Profile("test") // 테스트에서만 활성화
public class TestFailKafkaListener {

    @KafkaListener(
            topics = "order.orderCreate",
            groupId = "test-fail-listener",
            containerFactory = "paymentKafkaListenerContainerFactory"
    )
    public void consumeAndFail(String message) {
        throw new RuntimeException("강제 시스템 예외");
    }
}

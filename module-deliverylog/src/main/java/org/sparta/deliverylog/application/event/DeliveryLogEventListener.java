package org.sparta.deliverylog.application.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryLogEventListener {

    /**
     * 배송 생성 이벤트 수신 (Delivery 서비스로부터)
     */
    @KafkaListener(topics = "delivery.created", groupId = "delivery-log-service")
    public void handleDeliveryCreated(Map<String, Object> event) {
        log.info("Kafka 이벤트 수신 [delivery.created]: {}", event);

        // TODO: 배송 생성 시 자동으로 경로 생성 로직 추가 가능
        // deliveryLogService.createDeliveryLogFromDelivery(event);
    }

    /**
     * 배송 취소 이벤트 수신
     */
    @KafkaListener(topics = "delivery.canceled", groupId = "delivery-log-service")
    public void handleDeliveryCanceled(Map<String, Object> event) {
        log.info("Kafka 이벤트 수신 [delivery.canceled]: {}", event);

        // TODO: 배송 취소 시 관련 경로도 취소 처리
        // deliveryLogService.cancelByDeliveryId(event.get("deliveryId"));
    }
}

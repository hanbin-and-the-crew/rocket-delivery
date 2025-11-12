package org.sparta.company.infrastructure.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.sparta.common.event.hub.HubCreatedEvent;
import org.sparta.common.event.hub.HubDeletedEvent;
import org.sparta.common.event.hub.HubUpdatedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HubEventListener {

    @KafkaListener(topics = "hub-events", groupId = "company-service")
    public void listen(ConsumerRecord<String, Object> record) {
        Object event = record.value();

        if (event instanceof HubCreatedEvent e) {
            log.info("허브 생성 이벤트 수신 - id:{}, name:{}", e.hubId(), e.name());
        } else if (event instanceof HubUpdatedEvent e) {
            log.info("허브 수정 이벤트 수신 - id:{}, name:{}", e.hubId(), e.name());
        } else if (event instanceof HubDeletedEvent e) {
            log.info("허브 삭제 이벤트 수신 - id:{}", e.hubId());
        } else {
            log.warn("알 수 없는 이벤트 수신: {}", event.getClass().getSimpleName());
        }
    }
}

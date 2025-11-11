package org.sparta.company.infrastructure.event;

import lombok.extern.slf4j.Slf4j;
import org.sparta.hub.domain.event.HubCreatedEvent;
import org.sparta.hub.domain.event.HubDeletedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 허브 이벤트 소비자 (Kafka Listener)
 * 허브 생성/삭제 이벤트를 수신하여 회사 서비스에 반영
 */
@Slf4j
@Component
public class HubEventListener {

    @KafkaListener(topics = "hub-events", groupId = "company-service", containerFactory = "kafkaListenerContainerFactory")
    public void handleHubCreated(HubCreatedEvent event) {
        log.info("✅ 허브 생성 이벤트 수신 - 허브명: {}, 지역: {}", event.name(), event.region());
        // TODO: 허브 생성 시 관련 회사 데이터 업데이트 로직 추가
    }

    @KafkaListener(topics = "hub-events", groupId = "company-service", containerFactory = "kafkaListenerContainerFactory")
    public void handleHubDeleted(HubDeletedEvent event) {
        log.info("⚠️ 허브 삭제 이벤트 수신 - 허브 ID: {}", event.hubId());
        // TODO: 허브 삭제 시 관련 회사 비활성화 로직 추가
    }
}

package org.sparta.common.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 도메인 이벤트 발행 래퍼 클래스
 *
 * ApplicationEventPublisher를 직접 사용하는 대신 래퍼를 사용하면:
 * - 이벤트 발행 전후로 공통 로직(로깅, 이벤트 저장 등)을 추가할 수 있습니다.
 * - 내부 이벤트는 Spring Event, 외부 이벤트는 Kafka로 자동 발행합니다.
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "app.eventpublisher", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Autowired(required = false)
    private KafkaTemplate<String, Object> kafkaTemplate;

    public EventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * 로컬 이벤트 발행
     */
    public void publishLocal(DomainEvent event) {
        log.debug("로컬 이벤트 발행 - Type: {}, EventId: {}",
            event.eventType(),
            event.eventId()
        );

        applicationEventPublisher.publishEvent(event);

        log.trace("로컬 이벤트 발행 완료 - EventId: {}", event.eventId());
    }

    /**
     * Kafka
     */
    public void publishExternal(DomainEvent event) {
        log.debug("외부 이벤트 발행 - Type: {}, EventId: {}",
            event.eventType(),
            event.eventId()
        );

        if (kafkaTemplate == null) {
            log.warn("KafkaTemplate이 없습니다. 외부 이벤트를 발행할 수 없습니다: {}",
                event.eventType());
            return;
        }

        // Kafka Topic 이름은 이벤트 타입 기반
        String topic = determineTopicName(event);

        kafkaTemplate.send(topic, event.eventId().toString(), event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Kafka 이벤트 발행 실패 - EventId: {}", event.eventId(), ex);
                } else {
                    log.trace("Kafka 이벤트 발행 완료 - Topic: {}, EventId: {}",
                        topic, event.eventId());
                }
            });
    }


    /**
     * Selects the Kafka topic name for a domain event based on its event type.
     *
     * Maps event type prefixes and specific event names to topic names (for example: "User*" -> "user-events", "Planning*" -> "planning-events", "Message*" -> "message-events",
     * "Hub*" -> "hub-events", "Order*" -> "order-events", "OrderCreatedEvent" -> "order-created", "OrderCancelledEvent" -> "order-cancelled", "Stock*" -> "stock-events").
     * Defaults to "domain-events" when no mapping matches.
     *
     * @param event the domain event whose eventType() is used to determine the topic
     * @return the Kafka topic name for the given event
     */
    private String determineTopicName(DomainEvent event) {
        String eventType = event.eventType();

        // User 관련 이벤트
        if (eventType.startsWith("User")) {
            return "user-events";
        }
        // Planning 관련 이벤트
        if (eventType.startsWith("Planning")) {
            return "planning-events";
        }
        // Message 관련 이벤트
        if (eventType.startsWith("Message")) {
            return "message-events";
        }

        // Hub 관련 이벤트
        if (eventType.startsWith("Hub")) {
            return "hub-events";
        }

        // Order 관련 이벤트
        if (eventType.startsWith("Order")) {
            return "order-events";
        }
        // (stock 쪽 KafkaListener 기준으로 작성)
        if (eventType.equals("OrderCreatedEvent")) {
            return "order-created";
        }
        if (eventType.equals("OrderCancelledEvent")) {
            return "order-cancelled";
        }

        // Stock 관련 이벤트 (StockConfirmedEvent, StockReservedEvent, …)
        if (eventType.startsWith("Stock")) {
            return "stock-events";
        }


        // 기본 토픽
        return "domain-events";
    }
}
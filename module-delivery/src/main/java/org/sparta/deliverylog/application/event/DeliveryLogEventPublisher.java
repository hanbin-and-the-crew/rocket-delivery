//package org.sparta.deliverylog.application.event;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.sparta.deliverylog.domain.entity.DeliveryLog;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.stereotype.Component;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class DeliveryLogEventPublisher {
//
//    private final KafkaTemplate<String, Object> kafkaTemplate;
//
//    private static final String TOPIC_DELIVERY_LOG_CREATED = "delivery-log.created";
//    private static final String TOPIC_DELIVERY_MAN_ASSIGNED = "delivery-log.man-assigned";
//    private static final String TOPIC_DELIVERY_STARTED = "delivery-log.started";
//    private static final String TOPIC_DELIVERY_COMPLETED = "delivery-log.completed";
//    private static final String TOPIC_DELIVERY_CANCELED = "delivery-log.canceled";
//
//    /**
//     * 배송 경로 생성 이벤트
//     */
//    public void publishDeliveryLogCreated(DeliveryLog deliveryLog) {
//        Map<String, Object> event = createEventPayload(deliveryLog);
//        kafkaTemplate.send(TOPIC_DELIVERY_LOG_CREATED, deliveryLog.getDeliveryLogId().toString(), event);
//        log.info("Kafka 이벤트 발행 [{}]: {}", TOPIC_DELIVERY_LOG_CREATED, deliveryLog.getDeliveryLogId());
//    }
//
//    /**
//     * 배송 담당자 배정 이벤트
//     */
//    public void publishDeliveryManAssigned(DeliveryLog deliveryLog) {
//        Map<String, Object> event = createEventPayload(deliveryLog);
//        kafkaTemplate.send(TOPIC_DELIVERY_MAN_ASSIGNED, deliveryLog.getDeliveryLogId().toString(), event);
//        log.info("Kafka 이벤트 발행 [{}]: {}", TOPIC_DELIVERY_MAN_ASSIGNED, deliveryLog.getDeliveryLogId());
//    }
//
//    /**
//     * 배송 시작 이벤트
//     */
//    public void publishDeliveryStarted(DeliveryLog deliveryLog) {
//        Map<String, Object> event = createEventPayload(deliveryLog);
//        kafkaTemplate.send(TOPIC_DELIVERY_STARTED, deliveryLog.getDeliveryLogId().toString(), event);
//        log.info("Kafka 이벤트 발행 [{}]: {}", TOPIC_DELIVERY_STARTED, deliveryLog.getDeliveryLogId());
//    }
//
//    /**
//     * 배송 완료 이벤트
//     */
//    public void publishDeliveryCompleted(DeliveryLog deliveryLog) {
//        Map<String, Object> event = createEventPayload(deliveryLog);
//        event.put("actualDistance", deliveryLog.getActualDistance() != null ?
//                deliveryLog.getActualDistance().getValue() : null);
//        event.put("actualTime", deliveryLog.getActualTime() != null ?
//                deliveryLog.getActualTime().getValue() : null);
//
//        kafkaTemplate.send(TOPIC_DELIVERY_COMPLETED, deliveryLog.getDeliveryLogId().toString(), event);
//        log.info("Kafka 이벤트 발행 [{}]: {}", TOPIC_DELIVERY_COMPLETED, deliveryLog.getDeliveryLogId());
//    }
//
//    /**
//     * 배송 취소 이벤트
//     */
//    public void publishDeliveryCanceled(DeliveryLog deliveryLog) {
//        Map<String, Object> event = createEventPayload(deliveryLog);
//        kafkaTemplate.send(TOPIC_DELIVERY_CANCELED, deliveryLog.getDeliveryLogId().toString(), event);
//        log.info("Kafka 이벤트 발행 [{}]: {}", TOPIC_DELIVERY_CANCELED, deliveryLog.getDeliveryLogId());
//    }
//
//    // ========== Private Methods ==========
//
//    private Map<String, Object> createEventPayload(DeliveryLog deliveryLog) {
//        Map<String, Object> payload = new HashMap<>();
//        payload.put("deliveryLogId", deliveryLog.getDeliveryLogId());
//        payload.put("deliveryId", deliveryLog.getDeliveryId());
//        payload.put("hubSequence", deliveryLog.getHubSequence());
//        payload.put("departureHubId", deliveryLog.getDepartureHubId());
//        payload.put("destinationHubId", deliveryLog.getDestinationHubId());
//        payload.put("deliveryManId", deliveryLog.getDeliveryManId());
//        payload.put("status", deliveryLog.getDeliveryStatus().name());
//        payload.put("timestamp", System.currentTimeMillis());
//        return payload;
//    }
//}

//package org.sparta.deliveryman.domain.event;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.RequiredArgsConstructor;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.stereotype.Component;
//
//@Component
//@RequiredArgsConstructor
//public class DeliveryManEventPublisher {
//
//    private static final Logger logger = LoggerFactory.getLogger(DeliveryManEventPublisher.class);
//    private final KafkaTemplate<String, String> kafkaTemplate;
//    private final ObjectMapper objectMapper;
//    private static final String DELIVERY_MAN_TOPIC = "delivery-man-events";
//    private static final String HUB_MANAGER_ASSIGN_TOPIC = "hub-delivery-manager-assigned";
//    private static final String PARTNER_MANAGER_ASSIGN_TOPIC = "partner-delivery-manager-assigned";
//
//    public void publishCreatedEvent(DeliveryManCreatedEvent event) {
//        publishEvent(event, DELIVERY_MAN_TOPIC);
//    }
//
//    public void publishUpdatedEvent(DeliveryManUpdatedEvent event) {
//        publishEvent(event, DELIVERY_MAN_TOPIC);
//    }
//
//    public void publishDeletedEvent(DeliveryManDeletedEvent event) {
//        publishEvent(event, DELIVERY_MAN_TOPIC);
//    }
//
//    public void publishHubDeliveryManagerAssignedEvent(HubDeliveryManagerAssignedEvent event) {
//        publishEvent(event, HUB_MANAGER_ASSIGN_TOPIC);
//    }
//
//    public void publishPartnerDeliveryManagerAssignedEvent(PartnerDeliveryManagerAssignedEvent event) {
//        publishEvent(event, PARTNER_MANAGER_ASSIGN_TOPIC);
//    }
//
//    private void publishEvent(Object event, String topic) {
//        try {
//            String json = objectMapper.writeValueAsString(event);
//            kafkaTemplate.send(topic, json);
//            logger.info("Published event to topic {}: {}", topic, json);
//        } catch (JsonProcessingException e) {
//            logger.error("Failed to serialize event: {}", e.getMessage(), e);
//        }
//    }
//}

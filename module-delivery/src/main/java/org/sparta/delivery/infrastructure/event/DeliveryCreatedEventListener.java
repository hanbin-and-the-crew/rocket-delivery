//package org.sparta.delivery.infrastructure.event;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.sparta.delivery.domain.event.DeliveryCreatedEvent;
//import org.sparta.deliveryman.application.DeliveryManAssignmentService;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.stereotype.Component;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class DeliveryCreatedEventListener {
//
//    private final DeliveryManAssignmentService deliveryManAssignmentService;
//
//    @KafkaListener(topics = "delivery-created", groupId = "delivery-service")
//    public void onDeliveryCreated(DeliveryCreatedEvent event) {
//        log.info("Consume DeliveryCreatedEvent deliveryId={}", event.deliveryId());
//
//        deliveryManAssignmentService.assignForNewDelivery(event.deliveryId());
//    }
//}

//package org.sparta.deliveryman.infrastructure.event;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.sparta.delivery.infrastructure.event.DeliveryCreatedEvent;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.stereotype.Component;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class DeliveryManAssignmentListener {
//
//    private final ObjectMapper objectMapper;
//    private final DeliveryManAssignmentService assignmentService;
//
//    @KafkaListener(
//            topics = "delivery.created",          // TODO: 실제 토픽명으로 맞추기
//            groupId = "deliveryman-assignment"    // TODO: 그룹ID도 프로젝트 규칙에 맞게
//    )
//    public void onDeliveryCreated(String message) {
//        try {
//            DeliveryCreatedEvent event =
//                    objectMapper.readValue(message, DeliveryCreatedEvent.class);
//
//            log.info("[DeliveryManAssignmentListener] consume DeliveryCreatedEvent. deliveryId={}, orderId={}",
//                    event.deliveryId(), event.orderId());
//
//            assignmentService.assignForNewDelivery(event);
//
//        } catch (Exception e) {
//            log.error("[DeliveryManAssignmentListener] failed to handle DeliveryCreatedEvent. payload={}", message, e);
//            // 여기서 예외를 던지면 Kafka 리트라이/재처리 전략에 따라 동작
//            // throw e;  // 필요하면 DLQ 전략에 맞게 조정
//        }
//    }
//}

package org.sparta.order.domain.saga;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderSagaTrackerListener {

    private final SagaStateRepository sagaStateRepo;

    @KafkaListener(topics = {
            "order.orderApprove", "order.orderCancel",
            "payment-events", "payment.paymentCancel.DLT",
            "order.orderApprove.DLT"
    }, groupId = "order-saga-tracker")
    public void trackSagaEvents(ConsumerRecord<String, String> record) {
        try {
            UUID orderId = extractOrderId(record.value());
            SagaState state = sagaStateRepo.findByOrderId(orderId)
                    .orElse(SagaState.builder().orderId(orderId).build());

            updateState(state, record.topic());
            sagaStateRepo.save(state);
        } catch (Exception e) {
            log.error("Saga tracking failed: {}", record.topic(), e);
        }
    }

    private UUID extractOrderId(String payload) {
        // JSON 파싱 최소 구현 (실제 이벤트에 맞게 수정)
        return UUID.fromString(payload.split("\"orderId\":\"")[1].split("\"")[0]);
    }

    private void updateState(SagaState state, String topic) {
        switch (topic) {
            case "order.orderApprove" -> {
                state.setOrderStatus("APPROVED");
                state.setOverallStatus("IN_PROGRESS");
            }
            case "payment-events" -> state.setPaymentStatus("COMPLETED");
            case "order.orderApprove.DLT", "payment.paymentCancel.DLT" ->
                    state.setOverallStatus("RECOVERING");
        }
        state.setUpdatedAt(LocalDateTime.now());
    }
}


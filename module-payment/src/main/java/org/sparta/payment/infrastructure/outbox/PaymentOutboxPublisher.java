package org.sparta.payment.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


import org.sparta.payment.domain.entity.PaymentOutbox;
import org.sparta.payment.domain.enumeration.OutboxStatus;
import org.sparta.payment.domain.repository.PaymentOutboxRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentOutboxPublisher {

    private final PaymentOutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final int MAX_RETRY = 5;
    private static final String TOPIC = "payment-events";

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void publishReadyEvents() {
        List<PaymentOutbox> events = outboxRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.READY);

        for (PaymentOutbox event : events) {
            try {
                // 파티션 키 = orderId
                String key = event.getAggregateId().toString();

                kafkaTemplate.send(TOPIC, key, event.getPayload())
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                // 성공
                                event.markSent();
                            } else {
                                // 실패
                                event.increaseRetry();
                                if (event.getRetryCount() >= MAX_RETRY) {
                                    event.markFailed();
                                }
                            }
                            outboxRepository.save(event);
                        });

            } catch (Exception e) {
                log.error("Unexpected publishing error", e);
            }
        }
    }
}

package org.sparta.product.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.sparta.product.application.service.OrderCancelledStockRestoreHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCancelledEventListener {

    private final ObjectMapper objectMapper;
    private final OrderCancelledStockRestoreHandler handler;

    @KafkaListener(topics = "order.orderCancel", groupId = "product-service")
    public void onMessage(ConsumerRecord<String, Object> record) {

        Map<String, Object> event = toEventMap(record);
        if (event == null) return;

        UUID eventId = parseUuid(event.get("eventId"));
        UUID orderId = parseUuid(event.get("orderId"));

        if (eventId == null || orderId == null) {
            log.warn("[OrderCancelledEventListener] eventId/orderId missing or invalid. topic={}, partition={}, offset={}, keys={}",
                    record.topic(), record.partition(), record.offset(), event.keySet());
            return;
        }

        try {
            handler.handle(eventId, orderId);
        } catch (Exception e) {
            log.error("[OrderCancelledEventListener] handler failed. eventId={}, orderId={}, topic={}, partition={}, offset={}",
                    eventId, orderId, record.topic(), record.partition(), record.offset(), e);
            throw e;
        }
    }

    private Map<String, Object> toEventMap(ConsumerRecord<String, Object> record) {
        Object raw = record.value();

        try {
            if (raw instanceof Map<?, ?> mapRaw) {
                @SuppressWarnings("unchecked")
                Map<String, Object> event = (Map<String, Object>) mapRaw;
                return event;
            }

            if (raw instanceof byte[] bytes) {
                Object obj = objectMapper.readValue(bytes, Object.class);
                if (obj instanceof Map<?, ?> mapObj) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> event = (Map<String, Object>) mapObj;
                    return event;
                }
                log.warn("[OrderCancelledEventListener] unexpected deserialized type from bytes: {}", obj == null ? "null" : obj.getClass());
                return null;
            }

            if (raw instanceof String s) {
                Object obj = objectMapper.readValue(s, Object.class);
                if (obj instanceof Map<?, ?> mapObj) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> event = (Map<String, Object>) mapObj;
                    return event;
                }
                log.warn("[OrderCancelledEventListener] unexpected deserialized type from string: {}", obj == null ? "null" : obj.getClass());
                return null;
            }

            log.warn("[OrderCancelledEventListener] unsupported payload type: {}", raw == null ? "null" : raw.getClass());
            return null;

        } catch (Exception e) {
            log.warn("[OrderCancelledEventListener] parse failed. topic={}, partition={}, offset={}",
                    record.topic(), record.partition(), record.offset(), e);
            return null;
        }
    }

    private UUID parseUuid(Object value) {
        if (value instanceof UUID u) return u;
        if (value instanceof String s) {
            try {
                return UUID.fromString(s);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }
}

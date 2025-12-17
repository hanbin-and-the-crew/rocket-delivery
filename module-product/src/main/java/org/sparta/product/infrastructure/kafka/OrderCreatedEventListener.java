package org.sparta.product.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.sparta.product.application.service.OrderCreatedStockReservationHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedEventListener {

    private final OrderCreatedStockReservationHandler handler;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order.orderCreate", groupId = "product-service")
    public void onMessage(ConsumerRecord<String, byte[]> record) {

        Object raw;
        try {
            String json = new String(record.value(), StandardCharsets.UTF_8);
            raw = objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            log.warn("[OrderCreatedEventListener] json parse failed. topic={}, partition={}, offset={}",
                    record.topic(), record.partition(), record.offset(), e);
            return;
        }

        if (!(raw instanceof Map<?, ?> mapRaw)) {
            log.warn("[OrderCreatedEventListener] unexpected payload type: {}",
                    raw == null ? "null" : raw.getClass());
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> event = (Map<String, Object>) mapRaw;

        UUID upstreamEventId = resolveUpstreamEventId(event, record);

        UUID orderId = parseUuid(event.get("orderId"));
        if (orderId == null) {
            log.warn("[OrderCreatedEventListener] orderId missing or invalid. eventId={}, payloadKeys={}",
                    upstreamEventId, event.keySet());
            return;
        }

        // 외부 계약 reservationKey 변경 금지
        String externalReservationKey = orderId.toString();

        List<OrderCreatedStockReservationHandler.OrderLine> lines = extractOrderLines(event);
        if (lines.isEmpty()) {
            log.warn("[OrderCreatedEventListener] cannot extract product lines. eventId={}, orderId={}, payloadKeys={}",
                    upstreamEventId, orderId, event.keySet());
            return;
        }

        try {
            handler.handle(upstreamEventId, orderId, externalReservationKey, lines);
        } catch (Exception e) {
            log.warn("[OrderCreatedEventListener] handler failed. eventId={}, orderId={}",
                    upstreamEventId, orderId, e);
            throw e;
        }
    }

    private UUID resolveUpstreamEventId(Map<String, Object> event, ConsumerRecord<String, byte[]> record) {
        // 1) payload에 명확한 eventId(UUID 문자열)가 있으면 사용
        Object eventId = event.get("eventId");
        if (eventId instanceof String s && !s.isBlank()) {
            try {
                return UUID.fromString(s);
            } catch (IllegalArgumentException ignored) {
                // fallback으로 진행
            }
        }

        // 2) 없거나 UUID가 아니면 topic/partition/offset 기반 UUID 생성
        String fallback = record.topic() + ":" + record.partition() + ":" + record.offset();
        return UUID.nameUUIDFromBytes(fallback.getBytes(StandardCharsets.UTF_8));
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

    private Integer parseInt(Object value) {
        if (value instanceof Integer i) return i;
        if (value instanceof Number n) return n.intValue();
        if (value instanceof String s) {
            try {
                return Integer.parseInt(s);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private List<OrderCreatedStockReservationHandler.OrderLine> extractOrderLines(Map<String, Object> event) {
        List<OrderCreatedStockReservationHandler.OrderLine> lines = new ArrayList<>();

        // 1) 단건
        UUID productId = parseUuid(event.get("productId"));
        Integer quantity = parseInt(event.get("quantity"));
        if (productId != null && quantity != null) {
            lines.add(new OrderCreatedStockReservationHandler.OrderLine(productId, quantity));
            return lines;
        }

        // 2) 다건 items(List<Map>)
        Object itemsRaw = event.get("items");
        if (itemsRaw instanceof List<?> items) {
            for (Object it : items) {
                if (!(it instanceof Map<?, ?> m)) continue;

                UUID pid = parseUuid(m.get("productId"));
                Integer qty = parseInt(m.get("quantity"));
                if (pid == null || qty == null) {
                    return List.of(); // 추측 금지
                }
                lines.add(new OrderCreatedStockReservationHandler.OrderLine(pid, qty));
            }
            return lines;
        }

        return List.of();
    }
}

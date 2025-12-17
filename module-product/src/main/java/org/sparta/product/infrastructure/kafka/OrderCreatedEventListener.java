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

/**
 * order.orderCreate 소비
 *
 * 주의:
 * - "OrderCreatedEvent 클래스에 필드가 없다"는 현재 상황을 존중한다.
 * - 따라서 typed DTO로 역직렬화하지 않고, Map 기반으로 "확실히 파싱되는 케이스만" 처리한다.
 * - 파싱이 불확실하면 아무것도 하지 않는다(추측 금지).
 *
 * 변경점:
 * - All-or-Nothing + 실패 outbox 보장을 위해 실제 비즈니스 처리는 application 핸들러로 위임한다.
 */
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
            log.warn("[OrderCreatedEventListener] unexpected payload type: {}", raw == null ? "null" : raw.getClass());
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> event = (Map<String, Object>) mapRaw;

        String upstreamEventId = resolveUpstreamEventId(event, record);

        UUID orderId = parseUuid(event.get("orderId"));
        if (orderId == null) {
            log.warn("[OrderCreatedEventListener] orderId missing or invalid. eventId={}, payloadKeys={}",
                    upstreamEventId, event.keySet());
            return;
        }

        // 외부 계약 reservationKey 변경 금지: 현재 프로젝트 규칙은 orderId.toString()
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
            log.warn("[OrderCreatedEventListener] handler failed. eventId={}, orderId={}", upstreamEventId, orderId, e);
            throw e;
        }
    }

    private String resolveUpstreamEventId(Map<String, Object> event, ConsumerRecord<String, byte[]> record) {
        // 1) payload에 명확한 eventId가 있으면 사용
        Object eventId = event.get("eventId");
        if (eventId instanceof String s && !s.isBlank()) {
            return s;
        }
        // 2) 없으면 topic/partition/offset 기반으로 구성(동일 레코드 재소비에 대해 동일)
        return record.topic() + ":" + record.partition() + ":" + record.offset();
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
                if (!(it instanceof Map<?, ?>)) continue;
                Map<?, ?> m = (Map<?, ?>) it;

                UUID pid = parseUuid(m.get("productId"));
                Integer qty = parseInt(m.get("quantity"));
                if (pid == null || qty == null) {
                    return List.of(); // 하나라도 불확실하면 추측 금지
                }
                lines.add(new OrderCreatedStockReservationHandler.OrderLine(pid, qty));
            }
            return lines;
        }

        return List.of();
    }
}

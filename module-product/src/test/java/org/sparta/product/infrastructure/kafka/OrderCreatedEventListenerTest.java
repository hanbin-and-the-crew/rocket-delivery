package org.sparta.product.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.product.application.service.OrderCreatedStockReservationHandler;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.*;

class OrderCreatedEventListenerTest {

    @Test
    @DisplayName("정상 payload → handler 위임")
    void valid_event_calls_handler() throws Exception {
        OrderCreatedStockReservationHandler handler = mock(OrderCreatedStockReservationHandler.class);
        ObjectMapper objectMapper = new ObjectMapper();

        OrderCreatedEventListener listener =
                new OrderCreatedEventListener(handler, objectMapper);

        UUID orderId = UUID.randomUUID();

        String json = objectMapper.writeValueAsString(
                Map.of(
                        "eventId", "e1",
                        "orderId", orderId.toString(),
                        "productId", UUID.randomUUID().toString(),
                        "quantity", 2
                )
        );

        ConsumerRecord<String, byte[]> record =
                new ConsumerRecord<>("order.orderCreate", 0, 0L, null, json.getBytes(StandardCharsets.UTF_8));

        listener.onMessage(record);

        verify(handler).handle(eq("e1"), eq(orderId), eq(orderId.toString()), anyList());
    }
}

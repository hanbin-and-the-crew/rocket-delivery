package org.sparta.product.domain.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.common.event.product.StockConfirmedEvent;
import org.sparta.common.event.product.StockReservationFailedEvent;
import org.sparta.product.domain.outbox.OutboxStatus;
import org.sparta.product.domain.outbox.ProductOutboxEvent;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StockConfirmEventTest {

    @Test
    @DisplayName("ProductOutboxEvent.stockConfirmed: aggregate/eventType/status 세팅")
    void outbox_stockConfirmed_factory() {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        StockConfirmedEvent event = StockConfirmedEvent.of(orderId, productId, 3);
        ProductOutboxEvent outbox = ProductOutboxEvent.stockConfirmed(event, "{\"k\":\"v\"}");

        assertEquals("ORDER", outbox.getAggregateType());
        assertEquals(orderId, outbox.getAggregateId());
        assertEquals("STOCK_CONFIRMED", outbox.getEventType());
        assertEquals(OutboxStatus.READY, outbox.getStatus());
        assertEquals("{\"k\":\"v\"}", outbox.getPayload());
        assertNotNull(outbox.getCreatedAt());
        assertNotNull(outbox.getUpdatedAt());
    }

    @Test
    @DisplayName("ProductOutboxEvent.stockReservationFailed: aggregate/eventType/status 세팅")
    void outbox_stockReservationFailed_factory() {
        UUID orderId = UUID.randomUUID();

        StockReservationFailedEvent event = StockReservationFailedEvent.of(
                orderId,
                "externalKey",
                "product:insufficient_stock",
                "재고가 부족합니다"
        );

        ProductOutboxEvent outbox = ProductOutboxEvent.stockReservationFailed(event, "{\"err\":true}");

        assertEquals("ORDER", outbox.getAggregateType());
        assertEquals(orderId, outbox.getAggregateId());
        assertEquals("STOCK_RESERVATION_FAILED", outbox.getEventType());
        assertEquals(OutboxStatus.READY, outbox.getStatus());
        assertEquals("{\"err\":true}", outbox.getPayload());
        assertNotNull(outbox.getCreatedAt());
        assertNotNull(outbox.getUpdatedAt());
    }
}

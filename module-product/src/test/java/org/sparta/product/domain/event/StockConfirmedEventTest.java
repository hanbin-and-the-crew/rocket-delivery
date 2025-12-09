package org.sparta.product.domain.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StockConfirmedEventTest {

    @Test
    @DisplayName("StockConfirmedEvent.of() 사용 시 eventId가 자동 생성된다")
    void eventIdIsGenerated() {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        StockConfirmedEvent event = StockConfirmedEvent.of(orderId, productId, 5);

        assertThat(event.eventId()).isNotNull();
        assertThat(event.eventId()).isInstanceOf(UUID.class);
    }
}

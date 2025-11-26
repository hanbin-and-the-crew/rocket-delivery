package org.sparta.order.infrastructure.event.consumer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sparta.order.application.service.OrderService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockConfirmedEventListenerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private StockConfirmedEventListener listener;

    @Test
    @DisplayName("재고 확정 이벤트 수신 시 주문 승인(approveOrder)이 호출된다")
    void handleStockConfirmed_callsApproveOrder() {
        // given
        UUID orderId = UUID.randomUUID();
        StockConfirmedEvent event = new StockConfirmedEvent(
                UUID.randomUUID(),  // eventId
                orderId,
                UUID.randomUUID(),  // productId
                3,                  // confirmedQuantity
                10,                 // remainingQuantity
                Instant.now()
        );

        // when
        listener.handleStockConfirmed(event);

        // then
        ArgumentCaptor<UUID> captor = ArgumentCaptor.forClass(UUID.class);
        verify(orderService, times(1)).approveOrder(captor.capture());
        assertThat(captor.getValue()).isEqualTo(orderId);
    }
}

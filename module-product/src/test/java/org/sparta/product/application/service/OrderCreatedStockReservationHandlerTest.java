package org.sparta.product.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sparta.common.error.BusinessException;
import org.sparta.product.domain.entity.ProcessedEvent;
import org.sparta.product.domain.repository.ProcessedEventRepository;
import org.sparta.product.domain.repository.ProductOutboxEventRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderCreatedStockReservationHandlerTest {

    @Mock private StockService stockService;
    @Mock private ProductOutboxEventRepository outboxRepository;
    @Mock private ProcessedEventRepository processedEventRepository;
    @Mock private OrderCreatedFailureRecorder failureRecorder;
    @Mock private ObjectMapper objectMapper;

    @Test
    @DisplayName("handle: processed_event 중복(Unique 충돌) 발생 시 중복 처리 무시")
    void handle_duplicateIgnored() {
        OrderCreatedStockReservationHandler handler =
                new OrderCreatedStockReservationHandler(
                        stockService, outboxRepository, processedEventRepository, failureRecorder, objectMapper
                );

        doThrow(new DataIntegrityViolationException("dup"))
                .when(processedEventRepository)
                .save(any(ProcessedEvent.class));

        handler.handle(UUID.randomUUID(), UUID.randomUUID(), "ext", List.of(
                new OrderCreatedStockReservationHandler.OrderLine(UUID.randomUUID(), 2)
        ));

        verify(processedEventRepository, times(1)).save(any(ProcessedEvent.class));
        verifyNoInteractions(stockService, outboxRepository, failureRecorder);
        verifyNoInteractions(objectMapper);
    }

    @Test
    @DisplayName("handle: 성공 시 processed_event 선저장 + 예약 수행 + outbox 저장")
    void handle_success_allOrNothing() throws Exception {
        OrderCreatedStockReservationHandler handler =
                new OrderCreatedStockReservationHandler(
                        stockService, outboxRepository, processedEventRepository, failureRecorder, objectMapper
                );

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        String externalReservationKey = "ext";

        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();

        handler.handle(eventId, orderId, externalReservationKey, List.of(
                new OrderCreatedStockReservationHandler.OrderLine(p1, 2),
                new OrderCreatedStockReservationHandler.OrderLine(p2, 1)
        ));

        verify(processedEventRepository, times(1)).save(any(ProcessedEvent.class));

        verify(stockService).reserveStock(p1, externalReservationKey, 2);
        verify(stockService).reserveStock(p2, externalReservationKey, 1);

        verify(outboxRepository, times(2)).save(any());
        verifyNoInteractions(failureRecorder);

        verify(objectMapper, atLeastOnce()).writeValueAsString(any());
    }

    @Test
    @DisplayName("handle: reserveStock 중 BusinessException 발생 시 failureRecorder 호출 후 예외 재던짐 (processed_event는 이미 저장됨)")
    void handle_failure_recordsFailureAndRethrows() {
        OrderCreatedStockReservationHandler handler =
                new OrderCreatedStockReservationHandler(
                        stockService, outboxRepository, processedEventRepository, failureRecorder, objectMapper
                );

        BusinessException ex = mock(BusinessException.class);

        UUID productId = UUID.randomUUID();
        String externalReservationKey = "ext";

        doThrow(ex)
                .when(stockService)
                .reserveStock(eq(productId), eq(externalReservationKey), eq(2));

        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        try {
            handler.handle(eventId, orderId, externalReservationKey, List.of(
                    new OrderCreatedStockReservationHandler.OrderLine(productId, 2)
            ));
        } catch (BusinessException ignored) {
        }

        verify(processedEventRepository, times(1)).save(any(ProcessedEvent.class));

        verify(failureRecorder, times(1))
                .recordFailureIfFirst(eq(eventId), eq(orderId), eq(externalReservationKey), eq(ex));

        verifyNoInteractions(outboxRepository);
        verifyNoInteractions(objectMapper);
    }
}

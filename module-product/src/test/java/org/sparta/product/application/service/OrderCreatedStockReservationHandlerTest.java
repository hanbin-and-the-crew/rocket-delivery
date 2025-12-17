package org.sparta.product.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sparta.common.error.BusinessException;
import org.sparta.product.domain.entity.ProcessedEvent;
import org.sparta.product.domain.error.ProductErrorType;
import org.sparta.product.domain.outbox.ProductOutboxEvent;
import org.sparta.product.domain.repository.ProcessedEventRepository;
import org.sparta.product.domain.repository.ProductOutboxEventRepository;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderCreatedStockReservationHandlerTest {

    @Mock StockService stockService;
    @Mock ProductOutboxEventRepository outboxRepository;
    @Mock ProcessedEventRepository processedEventRepository;
    @Mock OrderCreatedFailureRecorder failureRecorder;
    @Mock ObjectMapper objectMapper;

    OrderCreatedStockReservationHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OrderCreatedStockReservationHandler(
                stockService,
                outboxRepository,
                processedEventRepository,
                failureRecorder,
                objectMapper
        );
    }

    @Test
    @DisplayName("handle: processedEvent 존재하면 중복 처리 무시")
    void handle_duplicateIgnored() {
        UUID eventId = UUID.randomUUID();

        when(processedEventRepository.existsByEventId(eventId)).thenReturn(true);

        handler.handle(eventId, UUID.randomUUID(), "ext", List.of());

        verifyNoInteractions(stockService, outboxRepository, failureRecorder);
        verify(processedEventRepository, times(1)).existsByEventId(eventId);
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("handle: All-or-Nothing 성공 -> reserve 모두 성공 후에만 outbox 저장 + processedEvent 저장")
    void handle_success_allOrNothing() throws Exception {
        UUID upstreamEventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        String externalKey = "orderKey";

        List<OrderCreatedStockReservationHandler.OrderLine> lines = List.of(
                new OrderCreatedStockReservationHandler.OrderLine(UUID.randomUUID(), 2),
                new OrderCreatedStockReservationHandler.OrderLine(UUID.randomUUID(), 3)
        );

        when(processedEventRepository.existsByEventId(upstreamEventId)).thenReturn(false);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"ok\":true}");

        handler.handle(upstreamEventId, orderId, externalKey, lines);

        // 1) reserve 호출 2회
        verify(stockService, times(2)).reserveStock(any(UUID.class), eq(externalKey), anyInt());

        // 2) outbox는 전체 성공 후에만 lines 개수만큼 저장
        verify(outboxRepository, times(2)).save(any(ProductOutboxEvent.class));

        // 3) processedEvent 저장(성공)
        ArgumentCaptor<ProcessedEvent> pe = ArgumentCaptor.forClass(ProcessedEvent.class);
        verify(processedEventRepository).save(pe.capture());
        assertEquals("OrderCreated", pe.getValue().getEventType());
        assertEquals(upstreamEventId, pe.getValue().getEventId());

        // 실패 기록기는 호출되지 않음
        verifyNoInteractions(failureRecorder);
    }

    @Test
    @DisplayName("handle: reserve 중 BusinessException 발생 -> failureRecorder 호출 + outbox/processedEvent(성공) 저장 없음")
    void handle_failure_recordsFailureAndRethrows() throws Exception {
        UUID upstreamEventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        String externalKey = "orderKey";

        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();

        List<OrderCreatedStockReservationHandler.OrderLine> lines = List.of(
                new OrderCreatedStockReservationHandler.OrderLine(productId1, 2),
                new OrderCreatedStockReservationHandler.OrderLine(productId2, 3)
        );

        when(processedEventRepository.existsByEventId(upstreamEventId)).thenReturn(false);

        BusinessException fail = new BusinessException(ProductErrorType.INSUFFICIENT_STOCK);
        doThrow(fail).when(stockService).reserveStock(eq(productId1), eq(externalKey), eq(2));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> handler.handle(upstreamEventId, orderId, externalKey, lines));
        assertSame(fail, ex);

        verify(failureRecorder).recordFailureIfFirst(eq(upstreamEventId), eq(orderId), eq(externalKey), eq(fail));

        verify(outboxRepository, never()).save(any());
        verify(processedEventRepository, never()).save(any(ProcessedEvent.class));
    }
}

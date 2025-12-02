package org.sparta.product.application.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sparta.common.error.BusinessException;
import org.sparta.common.event.EventPublisher;
import org.sparta.product.application.service.StockService;
import org.sparta.product.domain.entity.Stock;
import org.sparta.product.domain.enums.OutboxStatus;
import org.sparta.product.domain.event.ProductOutboxEvent;
import org.sparta.product.domain.repository.ProcessedEventRepository;
import org.sparta.product.infrastructure.event.kafka.dto.OrderCancelledEvent;
import org.sparta.product.infrastructure.event.kafka.dto.OrderCreatedEvent;
import org.sparta.product.infrastructure.event.kafka.dto.PaymentCompletedEvent;
import org.sparta.product.infrastructure.event.kafka.listener.StockEventHandler;
import org.sparta.product.infrastructure.event.kafka.dto.StockConfirmedEvent;
import org.sparta.product.infrastructure.event.kafka.dto.StockReservationCancelledEvent;
import org.sparta.product.infrastructure.event.kafka.dto.StockReservationFailedEvent;
import org.sparta.product.infrastructure.event.kafka.dto.StockReservedEvent;
import org.sparta.product.infrastructure.event.outbox.ProductOutboxEventRepository;
import org.sparta.product.support.fixtures.StockFixture;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockEventHandlerTest {

    @Mock
    private StockService stockService;

    @MockBean
    private EventPublisher eventPublisher;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private ProductOutboxEventRepository productOutboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private StockEventHandler stockEventHandler;

    @Test
    @DisplayName("주문 생성 이벤트 수신 시 재고 예약 + Outbox에 StockReservedEvent 저장")
    void handleOrderCreated_ShouldReserveStockAndSaveOutbox() throws Exception {

        // given
        UUID productId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        int orderQuantity = 30;

        Stock stock = StockFixture.withQuantity(100);
        OrderCreatedEvent event = new OrderCreatedEvent(
                eventId, orderId, productId, orderQuantity, UUID.randomUUID(), Instant.now());

        given(processedEventRepository.existsByEventId(eventId)).willReturn(false);
        given(stockService.getStock(productId)).willReturn(stock);
        given(objectMapper.writeValueAsString(any())).willReturn("{\"dummy\":\"payload\"}");


        // when
        stockEventHandler.handleOrderCreated(event);

        // then - 재고처리
        verify(stockService).reserveStock(productId, orderQuantity);
        verify(processedEventRepository).save(any());
        // then - 아웃박스 저장
        ArgumentCaptor<ProductOutboxEvent> captor = ArgumentCaptor.forClass(ProductOutboxEvent.class);
        verify(productOutboxEventRepository).save(captor.capture());

        ProductOutboxEvent saved = captor.getValue();
        assertThat(saved.getEventType()).isEqualTo(StockReservedEvent.class.getSimpleName());
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.READY);
        assertThat(saved.getAggregateId()).isEqualTo(productId);
        assertThat(saved.getPayload()).isNotBlank();
    }

    @Test
    @DisplayName("이미 처리된 OrderCreatedEvent이면 아무 작업도 하지 않는다 (멱등성)")
    void handleOrderCreated_Idempotent() {
        // given
        UUID productId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        OrderCreatedEvent event = new OrderCreatedEvent(
                eventId, orderId, productId, 10, UUID.randomUUID(), Instant.now()
        );
        given(processedEventRepository.existsByEventId(eventId)).willReturn(true);

        // when
        stockEventHandler.handleOrderCreated(event);

        // then
        verifyNoInteractions(stockService);
        verify(productOutboxEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("재고 부족 시 StockReservationFailedEvent가 Outbox에 저장된다")
    void handleOrderCreated_WithInsufficientStock_ShouldPublishFailedEvent() throws Exception {

        // given
        UUID productId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        int orderQuantity = 50;

        Stock stock = StockFixture.withQuantity(10);
        OrderCreatedEvent event = new OrderCreatedEvent(eventId, orderId, productId, orderQuantity, null, null);

        given(processedEventRepository.existsByEventId(eventId)).willReturn(false);
        given(stockService.getStock(productId)).willReturn(stock);
        doThrow(new BusinessException(org.sparta.product.domain.error.ProductErrorType.INSUFFICIENT_STOCK))
                .when(stockService).reserveStock(productId, orderQuantity);
        given(objectMapper.writeValueAsString(any())).willReturn("{\"dummy\":\"payload\"}");

        // when
        stockEventHandler.handleOrderCreated(event);

        // then
        ArgumentCaptor<ProductOutboxEvent> captor = ArgumentCaptor.forClass(ProductOutboxEvent.class);
        verify(productOutboxEventRepository).save(captor.capture());
        verify(processedEventRepository).save(any());

        ProductOutboxEvent saved = captor.getValue();
        assertThat(saved.getEventType()).isEqualTo(StockReservationFailedEvent.class.getSimpleName());
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.READY);
        assertThat(saved.getAggregateId()).isEqualTo(productId);
    }

    @Test
    @DisplayName("결제 완료 이벤트 수신 시 재고 확정  + Outbox에 StockConfirmedEvent 저장")
    void handlePaymentCompleted_ShouldConfirmStockAndPublishEvent() throws Exception{

        // given
        UUID productId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        int confirmedQuantity = 30;

        Stock stock = StockFixture.withQuantity(100);
        PaymentCompletedEvent event = new PaymentCompletedEvent(eventId, orderId, productId, confirmedQuantity, null);

        given(processedEventRepository.existsByEventId(eventId)).willReturn(false);
        given(stockService.getStock(productId)).willReturn(stock);
        given(objectMapper.writeValueAsString(any())).willReturn("{\"dummy\":\"payload\"}");

        // when
        stockEventHandler.handlePaymentCompleted(event);

        // then
        verify(stockService).confirmReservation(productId, confirmedQuantity);
        verify(processedEventRepository).save(any());

        ArgumentCaptor<ProductOutboxEvent> captor = ArgumentCaptor.forClass(ProductOutboxEvent.class);
        verify(productOutboxEventRepository).save(captor.capture());

        ProductOutboxEvent saved = captor.getValue();
        assertThat(saved.getEventType()).isEqualTo(StockConfirmedEvent.class.getSimpleName());
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.READY);
        assertThat(saved.getAggregateId()).isEqualTo(productId);
    }

    @Test
    @DisplayName("주문 취소 이벤트 수신 시 예약 취소 + Outbox에 StockReservationCancelledEvent 저장")
    void handleOrderCancelled_ShouldCancelReservationAndPublishEvent() throws Exception {

        // given
        UUID productId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        int cancelledQuantity = 30;

        OrderCancelledEvent event = new OrderCancelledEvent(eventId, orderId, productId, cancelledQuantity, null);

        given(processedEventRepository.existsByEventId(eventId)).willReturn(false);
        given(objectMapper.writeValueAsString(any())).willReturn("{\"dummy\":\"payload\"}");

        // when
        stockEventHandler.handleOrderCancelled(event);

        // then
        verify(stockService).cancelReservation(productId, cancelledQuantity);
        verify(processedEventRepository).save(any());

        ArgumentCaptor<ProductOutboxEvent> captor = ArgumentCaptor.forClass(ProductOutboxEvent.class);
        verify(productOutboxEventRepository).save(captor.capture());

        ProductOutboxEvent saved = captor.getValue();
        assertThat(saved.getEventType()).isEqualTo(StockReservationCancelledEvent.class.getSimpleName());
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.READY);
        assertThat(saved.getAggregateId()).isEqualTo(productId);
    }
}

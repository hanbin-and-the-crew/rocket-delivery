package org.sparta.product.application.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sparta.common.error.BusinessException;
import org.sparta.product.application.service.StockService;
import org.sparta.product.domain.entity.Stock;
import org.sparta.product.domain.repository.ProcessedEventRepository;
import org.sparta.product.infrastructure.event.OrderCancelledEvent;
import org.sparta.product.infrastructure.event.OrderCreatedEvent;
import org.sparta.product.infrastructure.event.PaymentCompletedEvent;
import org.sparta.product.infrastructure.event.publisher.StockConfirmedEvent;
import org.sparta.product.infrastructure.event.publisher.StockReservationCancelledEvent;
import org.sparta.product.infrastructure.event.publisher.StockReservationFailedEvent;
import org.sparta.product.infrastructure.event.publisher.StockReservedEvent;
import org.sparta.product.support.fixtures.ProductFixture;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

//테스트 목적 : 핸들러가 이벤트를 받았을 때 외부 의존에 대해 올바른 행위를 수행하는지 행위 검증을 합니다.
@ExtendWith(MockitoExtension.class)
class StockEventHandlerTest {

    @Mock
    private StockService stockService;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private StockEventHandler stockEventHandler;

    @Test
    @DisplayName("주문 생성 이벤트 수신 시 재고 예약 성공 후 StockReservedEvent 발행")
    void handleOrderCreated_ShouldReserveStockAndPublishEvent() {
        UUID productId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        int orderQuantity = 30;

        Stock stock = ProductFixture.withStock(100).getStock();
        OrderCreatedEvent event = new OrderCreatedEvent(eventId, orderId, productId, orderQuantity, null, null);

        given(processedEventRepository.existsByEventId(eventId)).willReturn(false);
        given(stockService.getStock(productId)).willReturn(stock);

        stockEventHandler.handleOrderCreated(event);

        verify(stockService).reserveStock(productId, orderQuantity);
        verify(processedEventRepository).save(any());
        verify(kafkaTemplate).send(eq("stock-reserved"), eq(orderId.toString()), any(StockReservedEvent.class));
    }

    @Test
    @DisplayName("재고 부족 시 StockReservationFailedEvent 발행")
    void handleOrderCreated_WithInsufficientStock_ShouldPublishFailedEvent() {
        UUID productId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        int orderQuantity = 50;

        Stock stock = ProductFixture.withStock(10).getStock();
        OrderCreatedEvent event = new OrderCreatedEvent(eventId, orderId, productId, orderQuantity, null, null);

        given(processedEventRepository.existsByEventId(eventId)).willReturn(false);
        given(stockService.getStock(productId)).willReturn(stock);
        doThrow(new BusinessException(org.sparta.product.domain.error.ProductErrorType.INSUFFICIENT_STOCK))
                .when(stockService).reserveStock(productId, orderQuantity);

        stockEventHandler.handleOrderCreated(event);

        verify(kafkaTemplate).send(eq("stock-reservation-failed"), eq(orderId.toString()), any(StockReservationFailedEvent.class));
        verify(processedEventRepository).save(any());
    }

    @Test
    @DisplayName("결제 완료 이벤트 수신 시 재고 확정 후 StockConfirmedEvent 발행")
    void handlePaymentCompleted_ShouldConfirmStockAndPublishEvent() {
        UUID productId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        int confirmedQuantity = 30;

        Stock stock = ProductFixture.withStock(100).getStock();
        stock.reserve(30);

        PaymentCompletedEvent event = new PaymentCompletedEvent(eventId, orderId, productId, confirmedQuantity, null);

        given(processedEventRepository.existsByEventId(eventId)).willReturn(false);
        given(stockService.getStock(productId)).willReturn(stock);

        stockEventHandler.handlePaymentCompleted(event);

        verify(stockService).confirmReservation(productId, confirmedQuantity);
        verify(processedEventRepository).save(any());
        verify(kafkaTemplate).send(eq("stock-confirmed"), eq(orderId.toString()), any(StockConfirmedEvent.class));
    }

    @Test
    @DisplayName("주문 취소 이벤트 수신 시 예약 취소 후 StockReservationCancelledEvent 발행")
    void handleOrderCancelled_ShouldCancelReservationAndPublishEvent() {
        UUID productId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        int cancelledQuantity = 30;

        OrderCancelledEvent event = new OrderCancelledEvent(eventId, orderId, productId, cancelledQuantity, null);

        given(processedEventRepository.existsByEventId(eventId)).willReturn(false);

        stockEventHandler.handleOrderCancelled(event);

        verify(stockService).cancelReservation(productId, cancelledQuantity);
        verify(processedEventRepository).save(any());
        verify(kafkaTemplate).send(eq("stock-reservation-cancelled"), eq(orderId.toString()), any(StockReservationCancelledEvent.class));
    }
}

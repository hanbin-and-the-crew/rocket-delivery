package org.sparta.product.infrastructure.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sparta.common.event.DomainEvent;
import org.sparta.common.event.EventPublisher;
import org.sparta.product.application.service.StockService;
import org.sparta.product.domain.entity.Stock;
import org.sparta.product.domain.entity.StockReservation;
import org.sparta.product.domain.event.StockConfirmedEvent;
import org.sparta.product.domain.repository.StockRepository;
import org.sparta.product.domain.repository.StockReservationRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.sparta.product.domain.outbox.ProductOutboxEvent;
import org.sparta.product.domain.repository.ProductOutboxEventRepository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * OrderCreatedEventListener 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class OrderCreatedEventListenerTest {

    @Mock
    private StockService stockService;

    @Mock
    private StockReservationRepository stockReservationRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private OrderCreatedEventListener listener;

    @Mock
    private ProductOutboxEventRepository productOutboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;


    @Test
    @DisplayName("정상적인 order-created 이벤트를 받으면 재고 확정 + Outbox 에 StockConfirmedEvent 저장")
    void handleOrderCreated_success() throws Exception {
        // given
        UUID orderId = UUID.randomUUID();
        String orderIdStr = orderId.toString();
        String reservationKey = orderIdStr;

        UUID stockId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        int reservedQuantity = 3;

        Map<String, Object> payload = Map.of(
                "orderId", orderIdStr
        );
        ConsumerRecord<String, Object> record =
                new ConsumerRecord<>("order-created", 0, 0L, null, payload);

        StockReservation reservation = mock(StockReservation.class);
        when(reservation.getStockId()).thenReturn(stockId);
        when(reservation.getReservedQuantity()).thenReturn(reservedQuantity);

        when(stockReservationRepository.findByReservationKey(reservationKey))
                .thenReturn(Optional.of(reservation));

        Stock stock = mock(Stock.class);
        when(stock.getProductId()).thenReturn(productId);

        when(stockRepository.findById(stockId))
                .thenReturn(Optional.of(stock));

        // ObjectMapper 직렬화 stub
        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"dummy\":\"json\"}");

        // when
        listener.handleOrderCreated(record);

        // then
        verify(stockService, times(1)).confirmReservation(reservationKey);

        ArgumentCaptor<ProductOutboxEvent> outboxCaptor =
                ArgumentCaptor.forClass(ProductOutboxEvent.class);

        verify(productOutboxEventRepository, times(1)).save(outboxCaptor.capture());

        ProductOutboxEvent saved = outboxCaptor.getValue();
        assertThat(saved.getAggregateId()).isEqualTo(orderId);
        assertThat(saved.getEventType()).isEqualTo("STOCK_CONFIRMED");
        assertThat(saved.getStatus()).isNotNull();
        assertThat(saved.getPayload()).isEqualTo("{\"dummy\":\"json\"}");

        // Publisher 가 담당하므로 여기서는 eventPublisher 를 직접 검증하지 않는다.
        verifyNoInteractions(eventPublisher);
    }


    @Test
    @DisplayName("orderId 가 없는 이벤트이면 아무 작업도 수행하지 않는다")
    void handleOrderCreated_whenOrderIdMissing_shouldDoNothing() {
        // given
        Map<String, Object> payload = Map.of(
                "foo", "bar"
        );
        ConsumerRecord<String, Object> record =
                new ConsumerRecord<>("order-created", 0, 0L, null, payload);

        // when
        listener.handleOrderCreated(record);

        // then
        verifyNoInteractions(
                stockService,
                stockReservationRepository,
                stockRepository,
                eventPublisher,
                productOutboxEventRepository
        );
    }




    @Test
    @DisplayName("재고 확정 중 예외가 발생하면 Outbox 에도 아무 이벤트를 저장하지 않는다")
    void handleOrderCreated_whenConfirmReservationFails_shouldNotPublishEvent() {
        // given
        UUID orderId = UUID.randomUUID();
        String orderIdStr = orderId.toString();
        String reservationKey = orderIdStr;

        Map<String, Object> payload = Map.of(
                "orderId", orderIdStr
        );
        ConsumerRecord<String, Object> record =
                new ConsumerRecord<>("order-created", 0, 0L, null, payload);

        doThrow(new RuntimeException("DB down or something"))
                .when(stockService)
                .confirmReservation(reservationKey);

        // when
        listener.handleOrderCreated(record);

        // then
        verify(stockService, times(1)).confirmReservation(reservationKey);
        verifyNoInteractions(stockReservationRepository, stockRepository, eventPublisher, productOutboxEventRepository);
    }


    @Test
    @DisplayName("reservationKey 로 예약을 찾지 못하면 Outbox 에 이벤트를 저장하지 않는다")
    void handleOrderCreated_whenReservationMissing_shouldNotPublishEvent() {
        // given
        UUID orderId = UUID.randomUUID();
        String orderIdStr = orderId.toString();
        String reservationKey = orderIdStr;

        Map<String, Object> payload = Map.of(
                "orderId", orderIdStr
        );
        ConsumerRecord<String, Object> record =
                new ConsumerRecord<>("order-created", 0, 0L, null, payload);

        when(stockReservationRepository.findByReservationKey(reservationKey))
                .thenReturn(Optional.empty());

        // when
        listener.handleOrderCreated(record);

        // then
        verify(stockService, times(1)).confirmReservation(reservationKey);
        verify(eventPublisher, never()).publishExternal(any());
        verify(productOutboxEventRepository, never()).save(any());
    }





}

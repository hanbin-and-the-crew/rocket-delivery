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

    @Test
    @DisplayName("정상적인 order-created 이벤트를 받으면 재고 확정 + StockConfirmedEvent 발행")
    void handleOrderCreated_success() {
        // given
        UUID orderId = UUID.randomUUID();
        String orderIdStr = orderId.toString();
        String reservationKey = orderIdStr;

        UUID stockId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        int reservedQuantity = 3;

        // Kafka ConsumerRecord 가 받은 payload 형태를 흉내낸다.
        Map<String, Object> payload = Map.of(
                "orderId", orderIdStr
        );
        ConsumerRecord<String, Object> record =
                new ConsumerRecord<>("order-created", 0, 0L, null, payload);

        // StockReservation, Stock 은 실제 엔티티 대신 mock 으로 대체
        StockReservation reservation = mock(StockReservation.class);
        when(reservation.getStockId()).thenReturn(stockId);
        when(reservation.getReservedQuantity()).thenReturn(reservedQuantity);

        when(stockReservationRepository.findByReservationKey(reservationKey))
                .thenReturn(Optional.of(reservation));

        Stock stock = mock(Stock.class);
        when(stock.getProductId()).thenReturn(productId);

        when(stockRepository.findById(stockId))
                .thenReturn(Optional.of(stock));

        // when
        listener.handleOrderCreated(record);

        // then
        // 1) 재고 확정 서비스가 올바른 reservationKey 로 호출되었는지
        verify(stockService, times(1)).confirmReservation(reservationKey);

        // 2) 외부 이벤트가 발행되었는지, 그 타입/내용이 올바른지
        ArgumentCaptor<DomainEvent> eventCaptor =
                ArgumentCaptor.forClass(DomainEvent.class);

        verify(eventPublisher, times(1)).publishExternal(eventCaptor.capture());

        DomainEvent published = eventCaptor.getValue();
        assertThat(published).isInstanceOf(StockConfirmedEvent.class);

        StockConfirmedEvent stockConfirmedEvent = (StockConfirmedEvent) published;
        assertThat(stockConfirmedEvent.orderId()).isEqualTo(orderId);
        assertThat(stockConfirmedEvent.productId()).isEqualTo(productId);
        assertThat(stockConfirmedEvent.confirmedQuantity()).isEqualTo(reservedQuantity);
        assertThat(stockConfirmedEvent.occurredAt()).isNotNull(); // 시간은 값만 존재하는지만 체크
    }

    @Test
    @DisplayName("orderId 가 없는 이벤트이면 아무 작업도 수행하지 않는다")
    void handleOrderCreated_whenOrderIdMissing_shouldDoNothing() {
        // given
        Map<String, Object> payload = Map.of(
                "foo", "bar"   // orderId 없음
        );
        ConsumerRecord<String, Object> record =
                new ConsumerRecord<>("order-created", 0, 0L, null, payload);

        // when
        listener.handleOrderCreated(record);

        // then
        // service / repository / publisher 가 전혀 호출되지 않아야 한다.
        verifyNoInteractions(stockService, stockReservationRepository, stockRepository, eventPublisher);
    }



    @Test
    @DisplayName("재고 확정 중 예외가 발생하면 이벤트를 발행하지 않는다")
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

        // StockService 가 예외를 던지는 상황을 시뮬레이트 (void 메서드이므로 doThrow 사용)
        doThrow(new RuntimeException("DB down or something"))
                .when(stockService)
                .confirmReservation(reservationKey);

        // when
        listener.handleOrderCreated(record);

        // then
        verify(stockService, times(1)).confirmReservation(reservationKey);
        verifyNoInteractions(stockReservationRepository, stockRepository, eventPublisher);
    }

    @Test
    @DisplayName("reservationKey 로 예약을 찾지 못하면 이벤트를 발행하지 않는다")
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

        // 예약이 없는 상황
        when(stockReservationRepository.findByReservationKey(reservationKey))
                .thenReturn(Optional.empty());

        // when
        listener.handleOrderCreated(record);

        // then
        // 재고확정은 호출되어야 하고,
        verify(stockService, times(1)).confirmReservation(reservationKey);
        // 이후 예약/Stock 조회 결과가 없으므로 이벤트 발행은 하면 안 된다.
        verify(stockRepository, never()).findById(any());
        verify(eventPublisher, never()).publishExternal(any());
    }




}

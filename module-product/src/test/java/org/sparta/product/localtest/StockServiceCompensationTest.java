package org.sparta.product.localtest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sparta.common.error.BusinessException;
import org.sparta.common.event.EventPublisher;
import org.sparta.product.application.service.StockService;
import org.sparta.product.domain.error.ProductErrorType;
import org.sparta.product.domain.outbox.OutboxStatus;
import org.sparta.product.domain.outbox.ProductOutboxEvent;
import org.sparta.product.domain.repository.ProductOutboxEventRepository;
import org.sparta.product.domain.repository.StockRepository;
import org.sparta.product.domain.repository.StockReservationRepository;
import org.sparta.product.infrastructure.kafka.OrderCreatedEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 주문 완료 이벤트 수신 후 재고 확정(차감) 단계에서
 * 재고 부족(INSUFFICIENT_STOCK)이 발생하면
 * Product 모듈이 StockReservationFailedEvent 를 Outbox 에 READY 상태로 저장하는지 검증한다.
 *
 * - 실제 Kafka 대신 ConsumerRecord 를 직접 만들어 리스너를 호출
 * - StockService 는 재고 부족 예외를 던지도록 mock 제어
 * - Outbox 에 저장된 ProductOutboxEvent 의 메타데이터 & payload 를 검증
 */
@ExtendWith(MockitoExtension.class)
class StockServiceCompensationTest {

    @Mock
    private StockService stockService;                        // 재고 확정 서비스(mock)

    @Mock
    private StockReservationRepository stockReservationRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private EventPublisher eventPublisher;                    // 정상 성공 이벤트 발행기(mock)

    @Mock
    private ProductOutboxEventRepository productOutboxEventRepository; // Outbox 저장소(mock)

    private ObjectMapper objectMapper;                        // JSON 직렬화용 ObjectMapper

    private OrderCreatedEventListener listener;               // 테스트 대상: Kafka 리스너

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        listener = new OrderCreatedEventListener(
                stockService,
                stockReservationRepository,
                stockRepository,
                eventPublisher,
                productOutboxEventRepository,
                objectMapper
        );
    }

    @Test
    @DisplayName("재고 부족으로 재고 확정 실패 시 StockReservationFailedEvent가 Outbox에 저장된다")
    void handleOrderCreated_insufficientStock_shouldCreateFailedOutboxEvent() {
        // given
        // 1) 주문 ID와 예약 키(reservationKey == orderId.toString())
        UUID orderId = UUID.randomUUID();
        String reservationKey = orderId.toString();

        // 2) order.orderCreate 토픽에서 들어온다고 가정하는 payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", orderId.toString());

        ConsumerRecord<String, Object> record =
                new ConsumerRecord<>("order.orderCreate", 0, 0L, null, payload);

        // 3) 재고 확정 시 재고 부족 예외가 발생하도록 mock 설정
        BusinessException insufficientStockException =
                new BusinessException(ProductErrorType.INSUFFICIENT_STOCK);

        doThrow(insufficientStockException)
                .when(stockService)
                .confirmReservation(reservationKey);

        // when
        // Kafka 컨슈머 컨테이너 대신 리스너 메서드를 직접 호출해서 시나리오 재현
        listener.handleOrderCreated(record);

        // then
        // Outbox 에 저장된 이벤트 캡쳐
        ArgumentCaptor<ProductOutboxEvent> captor =
                ArgumentCaptor.forClass(ProductOutboxEvent.class);

        verify(productOutboxEventRepository, times(1)).save(captor.capture());

        ProductOutboxEvent saved = captor.getValue();

        // 1) Outbox 메타데이터 검증
        assertThat(saved.getAggregateId())
                .as("Outbox.aggregateId 는 주문 ID 여야 한다")
                .isEqualTo(orderId);

        assertThat(saved.getAggregateType())
                .as("Outbox.aggregateType 은 'ORDER' 로 고정된다")
                .isEqualTo("ORDER");

        assertThat(saved.getEventType())
                .as("보상 시나리오 이벤트 타입은 STOCK_RESERVATION_FAILED 여야 한다")
                .isEqualTo("STOCK_RESERVATION_FAILED");

        assertThat(saved.getStatus())
                .as("발행 대기 상태로 저장되어야 하므로 READY 여야 한다")
                .isEqualTo(OutboxStatus.READY);

        // 2) payload(JSON 문자열) 검증
        assertThat(saved.getPayload())
                .as("payload 는 null 이 아니어야 한다")
                .isNotNull();

        // 간단히 문자열 기반으로 핵심 정보 포함 여부 확인
        assertThat(saved.getPayload())
                .as("에러 코드로 product:insufficient_stock 이 포함되어야 한다")
                .contains("product:insufficient_stock");

        assertThat(saved.getPayload())
                .as("reservationKey 도 payload 안에 포함되어야 한다")
                .contains(reservationKey);

        // 3) 실패 시에는 성공 이벤트가 외부로 발행되면 안 된다
        verify(eventPublisher, never()).publishExternal(any());
    }
}

package org.sparta.order.infrastructure.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sparta.common.error.BusinessException;
import org.sparta.order.application.service.OrderService;
import org.sparta.order.domain.entity.ProcessedEvent;
import org.sparta.order.domain.error.OrderErrorType;
import org.sparta.order.domain.repository.ProcessedEventRepository;
import org.sparta.order.presentation.dto.response.OrderResponse;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.*;

/**
 * Order Kafka Listener Test
 */
@ExtendWith(MockitoExtension.class)
class OrderEventListenerTest {

    @InjectMocks
    private OrderEventListener listener;

    @Mock
    private OrderService orderService;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        listener = new OrderEventListener(orderService, processedEventRepository, objectMapper);
    }

    public static UUID deliveryId() {
        return UUID.fromString("10000000-0000-0000-0000-000000000001");
    }
    public static UUID orderId() {
        return UUID.fromString("20000000-0000-0000-0000-000000000002");
    }
    public static UUID paymentId() {
        return UUID.fromString("30000000-0000-0000-0000-000000000003");
    }
    public static UUID customerId() {
        return UUID.fromString("40000000-0000-0000-0000-000000000004");
    }
    public static UUID eventId() {
        return UUID.fromString("50000000-0000-0000-0000-000000000005");
    }

    // ================================
    // 1~3: DeliveryCompleted 테스트
    // ================================

    @Test
    @DisplayName("[DeliveryCompleted] 정상 처리 시 → OrderService.deliveredOrder 호출 + ProcessedEvent 저장")
    void testDeliveryCompletedSuccess() throws Exception {
        var event = new DeliveryCompletedEvent(deliveryId(), orderId(), eventId(), Instant.now());
        String message = objectMapper.writeValueAsString(event);

        when(processedEventRepository.existsByEventId(eventId())).thenReturn(false);
        when(orderService.deliveredOrder(orderId())).thenReturn(
                new OrderResponse.Update(orderId(), "DELIVERED", null)
        );

        listener.handleDeliveryCompleted(message);

        verify(orderService, times(1)).deliveredOrder(orderId());
        verify(processedEventRepository, times(1)).save(any(ProcessedEvent.class));
    }

    @Test
    @DisplayName("[DeliveryCompleted] 이미 처리된 eventId → OrderService 호출 안함")
    void testDeliveryCompletedIdempotent() throws Exception {
        var event = new DeliveryCompletedEvent(deliveryId(), orderId(), eventId(), Instant.now());
        String message = objectMapper.writeValueAsString(event);

        when(processedEventRepository.existsByEventId(eventId())).thenReturn(true);

        listener.handleDeliveryCompleted(message);

        verify(orderService, never()).deliveredOrder(any());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("[DeliveryCompleted] BusinessException → ProcessedEvent 저장되지만 OrderService는 실패")
    void testDeliveryCompletedBusinessException() throws Exception {
        var event = new DeliveryCompletedEvent(deliveryId(), orderId(), eventId(), Instant.now());
        String message = objectMapper.writeValueAsString(event);

        when(processedEventRepository.existsByEventId(eventId())).thenReturn(false);
        when(orderService.deliveredOrder(orderId()))
                .thenThrow(new BusinessException(OrderErrorType.ORDER_ALREADY_DELIVERED));

        listener.handleDeliveryCompleted(message);

        verify(processedEventRepository, times(1)).save(any(ProcessedEvent.class));
    }

    // ================================
    // 4~6: DeliveryStarted 테스트
    // ================================

    @Test
    @DisplayName("[DeliveryStarted] 정상 처리 → shippedOrder 호출 + ProcessedEvent 저장")
    void testDeliveryStartedSuccess() throws Exception {
        var event = new DeliveryStartedEvent(deliveryId(), orderId(), eventId(), Instant.now());
        String message = objectMapper.writeValueAsString(event);

        when(processedEventRepository.existsByEventId(eventId())).thenReturn(false);
        when(orderService.shippedOrder(orderId())).thenReturn(
                new OrderResponse.Update(orderId(), "SHIPPED", null)
        );

        listener.handleDeliveryStarted(message);

        verify(orderService, times(1)).shippedOrder(orderId());
        verify(processedEventRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("[DeliveryStarted] 이미 처리된 이벤트 → 중복 실행 방지")
    void testDeliveryStartedIdempotent() throws Exception {
        var event = new DeliveryStartedEvent(deliveryId(), orderId(), eventId(), Instant.now());
        String message = objectMapper.writeValueAsString(event);

        when(processedEventRepository.existsByEventId(eventId())).thenReturn(true);

        listener.handleDeliveryStarted(message);

        verify(orderService, never()).shippedOrder(any());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("[DeliveryStarted] BusinessException 발생 → ProcessedEvent 저장 (재처리 방지)")
    void testDeliveryStartedBusinessException() throws Exception {
        var event = new DeliveryStartedEvent(deliveryId(), orderId(), eventId(), Instant.now());
        String message = objectMapper.writeValueAsString(event);

        when(processedEventRepository.existsByEventId(eventId())).thenReturn(false);
        when(orderService.shippedOrder(orderId()))
                .thenThrow(new BusinessException(OrderErrorType.ORDER_ALREADY_SHIPPED));

        listener.handleDeliveryStarted(message);

        verify(processedEventRepository, times(1)).save(any());
    }

    // ================================
    // 7~10: PaymentApproved 테스트
    // ================================

    @Test
    @DisplayName("[PaymentApproved] 정상 승인 처리 → approveOrder 호출 + ProcessedEvent 저장")
    void testPaymentApprovedSuccess() throws Exception {
        var event = new PaymentApprovedEvent(paymentId(), orderId(), customerId(), 10000L, eventId(), Instant.now());
        String message = objectMapper.writeValueAsString(event);

        when(processedEventRepository.existsByEventId(eventId())).thenReturn(false);

        listener.handlePaymentApproved(message);

        verify(orderService, times(1))
                .approveOrder(event.orderId(), event.paymentId());
        verify(processedEventRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("[PaymentApproved] 이미 처리된 eventId → approveOrder 호출 안됨")
    void testPaymentApprovedIdempotent() throws Exception {
        var event = new PaymentApprovedEvent(paymentId(), orderId(), customerId(), 10000L, eventId(), Instant.now());
        String message = objectMapper.writeValueAsString(event);

        when(processedEventRepository.existsByEventId(eventId())).thenReturn(true);

        listener.handlePaymentApproved(message);

        verify(orderService, never()).approveOrder(any(), any());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("[PaymentApproved] BusinessException 발생 → ProcessedEvent 저장됨")
    void testPaymentApprovedBusinessException() throws Exception {
        var event = new PaymentApprovedEvent(paymentId(), orderId(), customerId(), 10000L, eventId(), Instant.now());
        String message = objectMapper.writeValueAsString(event);

        when(processedEventRepository.existsByEventId(eventId())).thenReturn(false);

        doThrow(new BusinessException(OrderErrorType.PAYMENT_APPROVE_FAILED))
                .when(orderService).approveOrder(event.orderId(), event.paymentId());

        listener.handlePaymentApproved(message);

        verify(processedEventRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("[PaymentApproved] JSON 파싱 실패 → 처리 안하고 종료")
    void testPaymentApprovedInvalidJson() {
        String invalidMessage = "{ invalid json }";

        listener.handlePaymentApproved(invalidMessage);

        verify(orderService, never()).approveOrder(any(), any());
        verify(processedEventRepository, never()).save(any());
    }

}

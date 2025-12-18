package org.sparta.order.infrastructure.event.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.error.BusinessException;
import org.sparta.common.event.delivery.DeliveryCompletedEvent;
import org.sparta.common.event.delivery.DeliveryCreatedEvent;
import org.sparta.common.event.delivery.DeliveryStartedEvent;
import org.sparta.common.event.payment.GenericDomainEvent;
import org.sparta.common.event.payment.PaymentCompletedEvent;
import org.sparta.order.application.service.OrderService;
import org.sparta.order.domain.entity.ProcessedEvent;
import org.sparta.order.domain.repository.ProcessedEventRepository;
import org.sparta.order.presentation.dto.response.OrderResponse;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Order 서비스 이벤트 리스너
 * - Payment 이벤트: 결제 완료 처리
 * - Delivery 이벤트: 배송 상태에 따른 주문 상태 변경
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventListener {

    private final OrderService orderService;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    // ============================
    // Payment 이벤트 처리
    // ============================

    /**
     * PaymentCompletedEvent 수신 (payment-events 토픽)
     * - Payment 승인 완료 → Order APPROVED + OrderApprovedEvent 발행
     */
    @KafkaListener(
            topics = "payment-events",
            groupId = "order-service",
            containerFactory = "orderPaymentKafkaListenerContainerFactory"
    )
    @Transactional
    public void handlePaymentCompleted(String message) {

        GenericDomainEvent envelope;
        try {
            envelope = objectMapper.readValue(message, GenericDomainEvent.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize payment event: {}", message, e);
            return;
        }

        log.info("Received Payment Envelope: eventId={}, type={}",
                envelope.eventId(), envelope.eventType());

        // 이벤트 타입 필터링 (payment.orderCreate.paymentCompleted만 처리)
        if (!"payment.orderCreate.paymentCompleted".equals(envelope.eventType())) {
            log.debug("Ignoring non-payment-completed event: {}", envelope.eventType());
            return;
        }

        // 멱등성 체크
        if (processedEventRepository.existsByEventId(envelope.eventId())) {
            log.info("Already processed payment event: eventId={}", envelope.eventId());
            return;
        }

        try {
            PaymentCompletedEvent event = objectMapper.convertValue(
                    envelope.payload(),
                    PaymentCompletedEvent.class
            );

            log.info("Processing PaymentCompletedEvent: paymentId={}, orderId={}",
                    event.paymentId(), event.orderId());

            // Order 승인 처리 (CREATED → APPROVED)
            orderService.approveOrder(event.orderId(), event.paymentId());

            // 이벤트 처리 기록
            processedEventRepository.save(
                    ProcessedEvent.of(envelope.eventId(), "PaymentCompletedEvent")
            );

            log.info("Order approved successfully: orderId={}, paymentId={}",
                    event.orderId(), event.paymentId());

        } catch (BusinessException e) {
            // 비즈니스 예외는 재시도 방지
            processedEventRepository.save(
                    ProcessedEvent.of(envelope.eventId(), "PaymentCompletedEvent")
            );
            log.error("Business error handling payment event: {}", e.getMessage(), e);

        } catch (Exception e) {
            log.error("Unexpected error handling payment event", e);
            throw new RuntimeException("Order approval failed", e);
        }
    }

    // ============================
    // Delivery 이벤트 처리 (토픽 분리)
    // ============================

    /**
     * DeliveryCreatedEvent 수신 (delivery.deliveryCreate 토픽)
     * - APPROVED → PREPARING_SHIPMENT (배송 준비중)
     */
    @KafkaListener(
            topics = "delivery.deliveryCreate",
            groupId = "order-service",
            containerFactory = "orderDeliveryKafkaListenerContainerFactory"
    )
    @Transactional
    public void handleDeliveryCreated(DeliveryCreatedEvent event) {

        log.info("DeliveryCreatedEvent received: deliveryId={}, orderId={}, eventId={}",
                event.deliveryId(), event.orderId(), event.eventId());

        // 멱등성 체크
        if (processedEventRepository.existsByEventId(event.eventId())) {
            log.info("Already processed delivery created event: eventId={}", event.eventId());
            return;
        }

        try {
            // Order 배송 준비중 처리
            OrderResponse.Update result = orderService.preparingOrder(event.orderId());

            // 이벤트 처리 기록
            processedEventRepository.save(
                    ProcessedEvent.of(event.eventId(), "DeliveryCreatedEvent")
            );

            log.info("Order preparing shipment: orderId={}, message={}",
                    event.orderId(), result.message());

        } catch (BusinessException e) {
            // 비즈니스 예외는 재시도 방지
            processedEventRepository.save(
                    ProcessedEvent.of(event.eventId(), "DeliveryCreatedEvent")
            );
            log.error("Business error handling delivery created: {}", e.getMessage(), e);

        } catch (Exception e) {
            log.error("Unexpected error handling delivery created", e);
            throw new RuntimeException("Order preparing shipment failed", e);
        }
    }

    /**
     * DeliveryStartedEvent 수신 (delivery.deliveryStart 토픽)
     * - PREPARING_SHIPMENT → SHIPPING (배송 중)
     */
    @KafkaListener(
            topics = "delivery.deliveryStart",
            groupId = "order-service",
            containerFactory = "orderDeliveryKafkaListenerContainerFactory"
    )
    @Transactional
    public void handleDeliveryStarted(DeliveryStartedEvent event) {

        log.info("Received DeliveryStartedEvent: deliveryId={}, orderId={}, eventId={}",
                event.deliveryId(), event.orderId(), event.eventId());

        // 멱등성 체크
        if (processedEventRepository.existsByEventId(event.eventId())) {
            log.info("Already processed delivery started event: eventId={}", event.eventId());
            return;
        }

        try {
            // Order 배송 중 처리
            OrderResponse.Update result = orderService.shippedOrder(event.orderId());

            // 이벤트 처리 기록
            processedEventRepository.save(
                    ProcessedEvent.of(event.eventId(), "DeliveryStartedEvent")
            );

            log.info("Order shipped: orderId={}, message={}",
                    event.orderId(), result.message());

        } catch (BusinessException e) {
            // 비즈니스 예외는 재시도 방지
            processedEventRepository.save(
                    ProcessedEvent.of(event.eventId(), "DeliveryStartedEvent")
            );
            log.error("Business error handling delivery started: {}", e.getMessage(), e);

        } catch (Exception e) {
            log.error("Unexpected error handling delivery started", e);
            throw new RuntimeException("Order shipped failed", e);
        }
    }

    /**
     * DeliveryCompletedEvent 수신 (delivery.deliveryComplete 토픽)
     * - SHIPPING → DELIVERED (배송 완료)
     */
    @KafkaListener(
            topics = "delivery.deliveryComplete",
            groupId = "order-service",
            containerFactory = "orderDeliveryKafkaListenerContainerFactory"
    )
    @Transactional
    public void handleDeliveryCompleted(DeliveryCompletedEvent event) {

        log.info("Received DeliveryCompletedEvent: deliveryId={}, orderId={}, eventId={}",
                event.deliveryId(), event.orderId(), event.eventId());

        // 멱등성 체크
        if (processedEventRepository.existsByEventId(event.eventId())) {
            log.info("Already processed delivery completed event: eventId={}", event.eventId());
            return;
        }

        try {
            // Order 배송 완료 처리
            OrderResponse.Update result = orderService.deliveredOrder(event.orderId());

            // 이벤트 처리 기록
            processedEventRepository.save(
                    ProcessedEvent.of(event.eventId(), "DeliveryCompletedEvent")
            );

            log.info("Order delivered: orderId={}, message={}",
                    event.orderId(), result.message());

        } catch (BusinessException e) {
            // 비즈니스 예외는 재시도 방지
            processedEventRepository.save(
                    ProcessedEvent.of(event.eventId(), "DeliveryCompletedEvent")
            );
            log.error("Business error handling delivery completed: {}", e.getMessage(), e);

        } catch (Exception e) {
            log.error("Unexpected error handling delivery completed", e);
            throw new RuntimeException("Order delivered failed", e);
        }
    }
}

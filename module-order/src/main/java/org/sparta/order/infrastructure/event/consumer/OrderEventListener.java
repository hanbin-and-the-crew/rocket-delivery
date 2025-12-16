package org.sparta.order.infrastructure.event.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.error.BusinessException;
import org.sparta.common.event.payment.GenericDomainEvent;
import org.sparta.common.event.payment.PaymentCompletedEvent;
import org.sparta.order.application.service.OrderService;
import org.sparta.order.domain.entity.ProcessedEvent;
import org.sparta.order.domain.repository.ProcessedEventRepository;
import org.sparta.order.presentation.dto.response.OrderResponse;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;

/**
 * 배달 시작/완료 이벤트 수신
 * 결제 승인 이벤트 수신
 * 멱등성 보장 (ProcessedEvent)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventListener {

    private final OrderService orderService;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * [ DeliveryCompletedEvent 수신 ]
     * delivery 모듈에서 최종 배송 완료 이벤트를 받아서 Order 배송 완료 처리
     */
    @KafkaListener(
            topics = "delivery-events",  // Delivery 모듈의 토픽
            groupId = "order-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleDeliveryCompleted(String message) {

        DeliveryCompletedEvent event;

        try {
            event = objectMapper.readValue(message, DeliveryCompletedEvent.class);
        } catch (JsonProcessingException e) {
            log.error("Invalid DeliveryCompletedEvent message: {}", message, e);
            return;
        }

        log.info("Received DeliveryCompletedEvent: deliveryId={}, orderId={}, eventId={}",
                event.deliveryId(), event.orderId(), event.eventId());

        // 멱등성 체크
        if (processedEventRepository.existsByEventId(event.eventId())) {
            log.warn("이미 처리된 이벤트 - eventId: {}", event.eventId());
            return;
        }

        try {
            // Order 배송 완료 처리 (SHIPPING -> DELIVERED)
            OrderResponse.Update result = orderService.deliveredOrder(event.orderId());

            // 이벤트 처리 기록
            processedEventRepository.save(
                    ProcessedEvent.of(event.eventId(), "DeliveryCompletedEvent")
            );

            log.info("Order delivered: orderId={}, message={}",
                    event.orderId(),
                    result.message()  // 응답 메시지
            );

        } catch (BusinessException e) {
            // 비즈니스 예외는 이벤트 처리 완료로 간주 (재시도 방지)
            processedEventRepository.save(
                    ProcessedEvent.of(event.eventId(), "DeliveryCompletedEvent")
            );
            log.error("Failed to handle delivery completed event: {}", message, e);

        } catch (Exception e) {
            log.error("Failed to handle delivery completed event: {}", message, e);
            throw new RuntimeException("Order Delivery failed", e);
        }
    }

    /**
     * [ DeliveryStartedEvent 수신 ]
     * delivery 모듈에서 배송 출발 이벤트를 받아서 Order 출고 처리
     */
    @KafkaListener(
            topics = "delivery-events",  // Delivery 모듈의 토픽
            groupId = "order-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleDeliveryStarted(String message) {

        DeliveryStartedEvent event;

        try {
            event = objectMapper.readValue(message, DeliveryStartedEvent.class);
        } catch (JsonProcessingException e) {
            log.error("Invalid DeliveryStartedEvent message: {}", message, e);
            return;
        }

        log.info("Received DeliveryStartedEvent: deliveryId={}, orderId={}, eventId={}",
                event.deliveryId(), event.orderId(), event.eventId());

        // 멱등성 체크
        if (processedEventRepository.existsByEventId(event.eventId())) {
            log.warn("이미 처리된 이벤트 - eventId: {}", event.eventId());
            return;
        }

        try {
            // Order 출고 처리 (APPROVED → SHIPPING)
            OrderResponse.Update result = orderService.shippedOrder(event.orderId());

            // 이벤트 처리 기록
            processedEventRepository.save(
                    ProcessedEvent.of(event.eventId(), "DeliveryStartedEvent")
            );

            log.info("Order shipped: orderId={}, message={}",
                    event.orderId(),
                    result.message()  // 응답 메시지
            );

        } catch (BusinessException e) {
            // 비즈니스 예외는 이벤트 처리 완료로 간주 (재시도 방지)
            processedEventRepository.save(
                    ProcessedEvent.of(event.eventId(), "DeliveryStartedEvent")
            );
            log.error("Failed to handle delivery started event: {}", message, e);

        } catch (Exception e) {
            log.error("Failed to handle delivery started event: {}", message, e);
            throw new RuntimeException("Order shipping failed", e);
        }
    }

    /**
     * [ PaymentCompletedEvent 수신 ]
     * Payment 모듈에서 결제 승인 이벤트를 받아서 Order 승인 처리
     */
    @KafkaListener(
            topics = "payment-events",  // Payment 모듈의 토픽
            groupId = "order-service",
            containerFactory = "orderPaymentKafkaListenerContainerFactory" // Payment쪽은 팩토리 다르게 사용!
    )
    @Transactional
    public void handlePaymentApproved(String message) {

        GenericDomainEvent envelope = null;
        try {
            envelope = objectMapper.readValue(message, GenericDomainEvent.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        log.info("Received Envelope event: eventId={}, type={}",
                envelope.eventId(), envelope.eventType());

        // payload를 다시 원하는 타입으로 변환
        PaymentCompletedEvent event =
                objectMapper.convertValue(envelope.payload(), PaymentCompletedEvent.class);

        log.info("Received PaymentCompletedEvent: paymentId={}, orderId={}, eventId={}",
                event.paymentId(), event.orderId(), envelope.eventId());

        // 멱등성 체크
        if (processedEventRepository.existsByEventId(envelope.eventId())) {
            log.warn("이미 처리된 이벤트 - eventId: {}", envelope.eventId());
            return;
        }

        try {
            // Order 승인 처리 (PENDING → APPROVED) + OrderApprovedEvent 발행
            orderService.approveOrder(event.orderId(), event.paymentId());

            // 이벤트 처리 기록
            processedEventRepository.save(
                    ProcessedEvent.of(envelope.eventId(), "PaymentCompletedEvent")
            );

            log.info("Order approved successfully: orderId={}, paymentId={}, eventId={}",
                    event.orderId(), event.paymentId(),  envelope.eventId());

        } catch (BusinessException e) {
            // 비즈니스 예외는 이벤트 처리 완료로 간주 (재시도 방지)
            processedEventRepository.save(
                    ProcessedEvent.of(envelope.eventId(), "PaymentCompletedEvent")
            );
            log.error("Failed to handle payment approved event: {}", message, e);

        } catch (Exception e) {
            log.error("Failed to handle payment approved event: {}", message, e);
            throw new RuntimeException("Order approval failed", e);
        }
    }
}
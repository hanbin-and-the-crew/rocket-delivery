package org.sparta.delivery.infrastructure.event.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.delivery.application.service.DeliveryCancelRequestTxService;
import org.sparta.delivery.application.service.DeliveryService;
import org.sparta.delivery.domain.entity.DeliveryCancelRequest;
import org.sparta.delivery.domain.entity.DeliveryProcessedEvent;
import org.sparta.delivery.domain.repository.DeliveryCancelRequestRepository;
import org.sparta.delivery.domain.repository.DeliveryProcessedEventRepository;
import org.sparta.common.event.payment.GenericDomainEvent;
import org.sparta.common.event.payment.PaymentCanceledEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * PaymentCanceledEvent 수신 리스너
 * - Payment 결제 취소 완료 시 Delivery 취소 처리
 * - Cancel Request 패턴 적용 (유령 배송 방지)
 * - 멱등성 보장 (DeliveryProcessedEvent)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCancelledListener {

    private final DeliveryService deliveryService;
    private final DeliveryCancelRequestRepository cancelRequestRepository;
    private final DeliveryProcessedEventRepository deliveryProcessedEventRepository;
    private final ObjectMapper objectMapper;
    private final DeliveryCancelRequestTxService cancelRequestTxService;

    /**
     * PaymentCanceledEvent 수신
     * - Cancel Request 저장 (무조건!)
     * - Delivery 있으면 즉시 취소, 없으면 리트라이
     */
    @KafkaListener(
            topics = "payment-events",
            groupId = "delivery-service",
            containerFactory = "deliveryPaymentKafkaListenerContainerFactory"
    )
    @Transactional
    public void handlePaymentCanceled(String message) {
        log.info("=== 🔥 PAYMENT EVENT TRIGGERED! message={}", message.substring(0, Math.min(200, message.length())));

        log.info("PaymentCanceledEvent received: message={}", message);

        // 1. 메시지 파싱
        GenericDomainEvent envelope;
        try {
            envelope = objectMapper.readValue(message, GenericDomainEvent.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize envelope: message={}", message, e);
            throw new RuntimeException("Failed to deserialize GenericDomainEvent", e);
        }

        // 2. 이벤트 타입 필터링
        if (!"payment.orderCancel.paymentCanceled".equals(envelope.eventType())) {
            log.debug("Ignoring non-cancel event: eventType={}", envelope.eventType());
            return;
        }

        log.info("Received Envelope event: eventId={}, type={}",
                envelope.eventId(), envelope.eventType());

        // 3. Payload 변환
        PaymentCanceledEvent event;
        try {
            event = objectMapper.convertValue(envelope.payload(), PaymentCanceledEvent.class);
        } catch (Exception e) {
            log.error("Failed to convert payload: eventId={}", envelope.eventId(), e);
            throw new RuntimeException("Failed to convert PaymentCanceledEvent", e);
        }

        log.info("Received PaymentCanceledEvent: paymentId={}, orderId={}, eventId={}",
                event.paymentId(), event.orderId(), envelope.eventId());

        // 4. 멱등성 체크 (전체 트랜잭션)
        if (deliveryProcessedEventRepository.existsByEventId(envelope.eventId())) {
            log.info("Event already processed, skipping: eventId={}, orderId={}",
                    envelope.eventId(), event.orderId());
            return;
        }

        try {
            // ===== Cancel Request 패턴 =====

            // 5. Cancel Request 저장 (별도 트랜잭션에서 수행)
            cancelRequestTxService.saveCancelRequestIfNotExists(
                    event.orderId(),
                    envelope.eventId()
            );

            // 6. Delivery 취소 시도
            boolean cancelled = deliveryService.cancelIfExists(event.orderId());

            if (cancelled) {
                cancelRequestRepository.findByOrderIdAndDeletedAtIsNull(event.orderId())
                        .ifPresent(request -> {
                            request.markApplied();
                            log.info("Cancel Request marked as APPLIED: orderId={}", event.orderId());
                        });

                log.info("Delivery cancelled immediately: orderId={}", event.orderId());
            } else {
                log.warn("Delivery not found yet, will retry: orderId={}", event.orderId());
                throw new DeliveryNotFoundYetException(event.orderId());
            }

            deliveryProcessedEventRepository.save(
                    DeliveryProcessedEvent.of(envelope.eventId(), "PAYMENT_CANCELED")
            );

            log.info("Payment cancellation processed successfully: orderId={}, eventId={}",
                    event.orderId(), envelope.eventId());

        } catch (DeliveryNotFoundYetException e) {
            log.warn("Delivery not found, will retry: orderId={}", event.orderId());
            throw e;

        } catch (Exception e) {
            log.error("Unexpected error processing payment cancellation: orderId={}, eventId={}",
                    event.orderId(), envelope.eventId(), e);
            throw e;
        }
    }


    /**
     * Delivery 없음 예외 (재시도용)
     */
    public static class DeliveryNotFoundYetException extends RuntimeException {
        private final UUID orderId;

        public DeliveryNotFoundYetException(UUID orderId) {
            super("Delivery not found yet for orderId: " + orderId);
            this.orderId = orderId;
        }

        public UUID getOrderId() {
            return orderId;
        }
    }
}

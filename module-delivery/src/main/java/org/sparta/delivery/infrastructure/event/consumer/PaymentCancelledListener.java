package org.sparta.delivery.infrastructure.event.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.error.BusinessException;
import org.sparta.common.event.payment.GenericDomainEvent;
import org.sparta.common.event.payment.PaymentCanceledEvent;
import org.sparta.delivery.domain.entity.Delivery;
import org.sparta.delivery.domain.entity.DeliveryProcessedEvent;
import org.sparta.delivery.domain.enumeration.DeliveryStatus;
import org.sparta.delivery.domain.error.DeliveryErrorType;
import org.sparta.delivery.domain.repository.DeliveryProcessedEventRepository;
import org.sparta.delivery.domain.repository.DeliveryRepository;
import org.sparta.deliverylog.application.service.DeliveryLogService;
import org.sparta.deliveryman.application.service.DeliveryManService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * PaymentCanceledEvent 수신 리스너
 * - Payment 결제 취소 완료 시 Delivery 취소 처리
 * - 멱등성 보장 (DeliveryProcessedEvent)
 * - 재시도 전략: Delivery 없으면 Kafka 자동 재시도
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCancelledListener {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryProcessedEventRepository deliveryProcessedEventRepository;
    private final DeliveryLogService deliveryLogService;
    private final DeliveryManService deliveryManService;
    private final ObjectMapper objectMapper;

    /**
     * PaymentCanceledEvent 수신
     * - Payment 모듈에서 결제 취소 완료 이벤트 수신
     * - Delivery, DeliveryLog, DeliveryMan 모두 취소 처리
     */
    @KafkaListener(
            topics = "payment-events",
            groupId = "delivery-service",
            containerFactory = "deliveryPaymentKafkaListenerContainerFactory"
    )
    @Transactional
    public void handlePaymentCanceled(String message) {
        log.info("PaymentCanceledEvent received: message={}", message);

        // 1. 메시지 파싱
        GenericDomainEvent envelope;
        try {
            envelope = objectMapper.readValue(message, GenericDomainEvent.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize envelope: message={}", message, e);
            throw new RuntimeException("Failed to deserialize GenericDomainEvent", e);
        }

        // ===== 2. 이벤트 타입 필터링 추가 =====  // 안하면 payment의 생성 이벤트까지 수신함
        if (!"payment.orderCancel.paymentCanceled".equals(envelope.eventType())) {
            log.debug("Ignoring non-cancel event: eventType={}", envelope.eventType());
            return; // 취소 이벤트가 아니면 무시
        }

        log.info("Received Envelope event: eventId={}, type={}",
                envelope.eventId(), envelope.eventType());

        // 2. Payload 변환
        PaymentCanceledEvent event;
        try {
            event = objectMapper.convertValue(envelope.payload(), PaymentCanceledEvent.class);
        } catch (Exception e) {
            log.error("Failed to convert payload: eventId={}", envelope.eventId(), e);
            throw new RuntimeException("Failed to convert PaymentCanceledEvent", e);
        }

        log.info("Received PaymentCanceledEvent: paymentId={}, orderId={}, eventId={}",
                event.paymentId(), event.orderId(), envelope.eventId());

        // 3. 멱등성 체크
        if (deliveryProcessedEventRepository.existsByEventId(envelope.eventId())) {
            log.info("Event already processed, skipping: eventId={}, orderId={}",
                    envelope.eventId(), event.orderId());
            return;
        }

        try {
            // 4. 배송 취소 처리
            processDeliveryCancellation(event, envelope.eventId());

        } catch (BusinessException e) {
            // Delivery 없으면 재시도
            if (e.getErrorType() == DeliveryErrorType.DELIVERY_NOT_FOUND) {
                log.warn("Delivery not found for cancelled order: orderId={} - Will retry",
                        event.orderId());
                throw e; // Kafka가 재시도
            }

            // 다른 BusinessException은 재시도 안 함 (이벤트 처리 완료로 간주)
            log.error("Business error processing payment cancellation: orderId={}, errorType={}",
                    event.orderId(), e.getErrorType(), e);

            // 이벤트 처리 완료 기록 (재시도 방지)
            deliveryProcessedEventRepository.save(
                    DeliveryProcessedEvent.of(envelope.eventId(), "PAYMENT_CANCELED")
            );

        } catch (Exception e) {
            log.error("Unexpected error processing payment cancellation: orderId={}",
                    event.orderId(), e);
            throw e; // 예기치 못한 에러는 재시도
        }
    }

    /**
     * 배송 취소 처리 핵심 로직
     */
    private void processDeliveryCancellation(PaymentCanceledEvent event, UUID eventId) {
        // 1. Delivery 조회
        Optional<Delivery> deliveryOpt =
                deliveryRepository.findByOrderIdAndDeletedAtIsNull(event.orderId());

        if (deliveryOpt.isEmpty()) {
            log.warn("Delivery not found for cancelled order: orderId={} - Throwing exception for retry",
                    event.orderId());

            // 재시도를 위해 예외 발생 (Kafka ErrorHandler가 재시도)
            throw new BusinessException(DeliveryErrorType.DELIVERY_NOT_FOUND);
        }

        Delivery delivery = deliveryOpt.get();

        // 2. 이미 취소된 경우 (멱등성)
        if (delivery.getStatus() == DeliveryStatus.CANCELED) {
            log.info("Delivery already cancelled: deliveryId={}, orderId={}",
                    delivery.getId(), event.orderId());

            // 이벤트 처리 완료 기록
            deliveryProcessedEventRepository.save(
                    DeliveryProcessedEvent.of(eventId, "PAYMENT_CANCELED")
            );
            return;
        }

        // 3. 취소 가능 여부 확인
        if (!canBeCancelled(delivery)) {
            log.warn("Delivery cannot be cancelled: deliveryId={}, status={}, orderId={}",
                    delivery.getId(), delivery.getStatus(), event.orderId());

            // 이미 배송 시작된 경우 - 이벤트는 처리 완료로 기록
            deliveryProcessedEventRepository.save(
                    DeliveryProcessedEvent.of(eventId, "PAYMENT_CANCELED")
            );
            return;
        }

        log.info("Starting delivery cancellation: deliveryId={}, orderId={}, status={}",
                delivery.getId(), event.orderId(), delivery.getStatus());

        // 4. Delivery 취소
        delivery.cancel();
        log.info("Delivery cancelled: deliveryId={}", delivery.getId());

        // 5. DeliveryLog 전체 취소
        cancelDeliveryLogs(delivery.getId());

        // 6. 배송 담당자 할당 해제
        unassignDeliveryMan(delivery);

        // 7. 이벤트 처리 완료 기록
        deliveryProcessedEventRepository.save(
                DeliveryProcessedEvent.of(eventId, "PAYMENT_CANCELED")
        );

        log.info("Delivery cancellation completed successfully: deliveryId={}, orderId={}",
                delivery.getId(), event.orderId());
    }

    /**
     * 취소 가능 여부 확인
     * - CREATED, HUB_WAITING 상태만 취소 가능
     */
    private boolean canBeCancelled(Delivery delivery) {
        DeliveryStatus status = delivery.getStatus();
        return status == DeliveryStatus.CREATED || status == DeliveryStatus.HUB_WAITING;
    }

    /**
     * DeliveryLog 전체 취소
     */
    private void cancelDeliveryLogs(UUID deliveryId) {
        try {
            deliveryLogService.cancelAllLogsByDeliveryId(deliveryId);
            log.info("All DeliveryLogs cancelled for deliveryId={}", deliveryId);
        } catch (Exception e) {
            log.error("Failed to cancel DeliveryLogs: deliveryId={}", deliveryId, e);
            // 로그 취소 실패해도 진행 (스케줄러가 나중에 처리)
        }
    }

    /**
     * 배송 담당자 할당 해제
     */
    private void unassignDeliveryMan(Delivery delivery) {
        if (delivery.getHubDeliveryManId() == null) {
            return;
        }

        try {
            deliveryManService.unassignDelivery(delivery.getHubDeliveryManId());
            log.info("DeliveryMan unassigned: deliveryManId={}, deliveryId={}",
                    delivery.getHubDeliveryManId(), delivery.getId());
        } catch (Exception e) {
            log.error("Failed to unassign DeliveryMan: deliveryManId={}, deliveryId={}",
                    delivery.getHubDeliveryManId(), delivery.getId(), e);
            // 담당자 해제 실패해도 진행 (스케줄러가 나중에 처리)
        }
    }
}

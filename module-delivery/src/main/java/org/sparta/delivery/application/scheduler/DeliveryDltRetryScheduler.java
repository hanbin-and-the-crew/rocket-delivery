package org.sparta.delivery.application.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.sparta.common.event.payment.PaymentCanceledEvent;
import org.sparta.delivery.application.service.DeliveryCancelRequestTxService;
import org.sparta.delivery.application.service.DeliveryService;
import org.sparta.delivery.domain.entity.DeliveryCancelRequest;
import org.sparta.delivery.domain.enumeration.CancelRequestStatus;
import org.sparta.delivery.domain.repository.DeliveryCancelRequestRepository;
import org.sparta.delivery.domain.repository.DeliveryProcessedEventRepository;
import org.sparta.common.event.payment.GenericDomainEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryDltRetryScheduler {

    private final DeliveryProcessedEventRepository deliveryProcessedEventRepository;
    private final DeliveryService deliveryService;
    private final DeliveryCancelRequestRepository cancelRequestRepository;
    private final DeliveryCancelRequestTxService cancelRequestTxService;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    /**
     * 5분마다 DLT 토픽 상태 모니터링 (메트릭스 로그)
     */
    @Scheduled(fixedRate = 300_000)
    @Transactional(readOnly = true)
    public void monitorDltTopics() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(1);
        long pendingCancelRequests = cancelRequestRepository.countPendingPaymentCancelDlt(
                CancelRequestStatus.REQUESTED, cutoffTime);

        log.info("DLT 모니터링: 대기중 CancelRequest={}, 1시간 내 발생, 시간={}",
                pendingCancelRequests,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    /**
     * PaymentCanceledEvent DLT 재처리
     * - PaymentCancelledListener와 동일한 패턴 적용
     * - 트랜잭션 분리로 Cancel Request 보존 보장
     */
    @KafkaListener(
            topics = "payment-events.DLT",
            groupId = "dlt-retry-group",
            containerFactory = "dltRetryKafkaListenerContainerFactory"
    )
    public void retryPaymentCancelDlt(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            String messageValue = record.value();

            //  이중 직렬화 처리
//            if (messageValue.startsWith("\"") && messageValue.endsWith("\"")) {
//                messageValue = messageValue.substring(1, messageValue.length() - 1)
//                        .replace("\\\"", "\"")  // \" → "
//                        .replace("\\\\", "\\"); // \\ → \
//            }
            //  개선된 이중 직렬화 처리
            // 1. 앞뒤 따옴표 제거 (작은따옴표 또는 큰따옴표)
            if ((messageValue.startsWith("\"") && messageValue.endsWith("\"")) ||
                    (messageValue.startsWith("'") && messageValue.endsWith("'"))) {
                messageValue = messageValue.substring(1, messageValue.length() - 1);
            }

            // 2. 이스케이프 문자 복원
            messageValue = messageValue
                    .replace("\\\"", "\"")  // \" → "
                    .replace("\\'", "'")    // \' → '
                    .replace("\\\\", "\\")  // \\ → \
                    .replace("\\n", "\n")   // \n → 개행
                    .replace("\\r", "\r")   // \r → 캐리지 리턴
                    .replace("\\t", "\t");  // \t → 탭

            log.debug("DLT 처리된 메시지 (100자): {}",
                    messageValue.substring(0, Math.min(100, messageValue.length())));


            // 1. GenericDomainEvent 파싱
            GenericDomainEvent envelope;
            try {
                envelope = OBJECT_MAPPER.readValue(messageValue, GenericDomainEvent.class);
            } catch (Exception e) {
                log.error("DLT 메시지 파싱 실패: offset={}", record.offset(), e);
                ack.acknowledge(); // 파싱 불가능 → 버림
                return;
            }

            // 2. eventType 필터링
            if (!"payment.orderCancel.paymentCanceled".equals(envelope.eventType())) {
                log.debug("DLT: 처리 대상 아님, eventType={}", envelope.eventType());
                ack.acknowledge();
                return;
            }

            UUID eventId = envelope.eventId();
            log.info("=== DLT 처리 시작: eventId={}, offset={}", eventId, record.offset());

            // 3. 멱등성 체크 (트랜잭션 없이)
            if (deliveryProcessedEventRepository.existsByEventId(eventId)) {
                log.info("DLT: 이미 처리됨: eventId={}", eventId);
                ack.acknowledge();
                return;
            }

            // 4. PaymentCanceledEvent 추출
            PaymentCanceledEvent event;
            try {
                event = OBJECT_MAPPER.convertValue(envelope.payload(), PaymentCanceledEvent.class);
            } catch (Exception e) {
                log.error("DLT: Payload 변환 실패: eventId={}", eventId, e);
                ack.acknowledge();
                return;
            }

            UUID orderId = event.orderId();
            log.info("DLT: PaymentCanceledEvent 처리 - orderId={}, eventId={}", orderId, eventId);

            // ===== PaymentCancelledListener와 동일한 패턴 =====

            // 5. Cancel Request 저장 (별도 트랜잭션, 이미 있으면 스킵)
            cancelRequestTxService.saveCancelRequestIfNotExists(orderId, eventId);

            // 6. CancelRequest 상태 확인
            Optional<DeliveryCancelRequest> cancelRequestOpt =
                    cancelRequestRepository.findByOrderIdAndDeletedAtIsNull(orderId);

            if (cancelRequestOpt.isEmpty()) {
                log.warn("DLT: CancelRequest 없음 (이상한 상황): orderId={}", orderId);
                ack.acknowledge();
                return;
            }

            DeliveryCancelRequest cancelRequest = cancelRequestOpt.get();

            // 7. 이미 APPLIED면 ProcessedEvent만 저장하고 스킵
            if (cancelRequest.getStatus() == CancelRequestStatus.APPLIED) {
                log.info("DLT: CancelRequest 이미 APPLIED: orderId={}", orderId);
                cancelRequestTxService.saveProcessedEvent(eventId, "PAYMENT_CANCELED_DLT");
                ack.acknowledge();
                return;
            }

            // 8. 배송 취소 시도 (별도 트랜잭션)
            boolean cancelled = deliveryService.cancelIfExists(orderId);

            if (cancelled) {
                // Cancel Request를 APPLIED로 마킹 (별도 트랜잭션)
                cancelRequestTxService.markCancelRequestAsApplied(orderId);
                log.info("DLT: 유령 배송 처리 완료 (배송 취소): orderId={}", orderId);
            } else {
                // 배송 없음 → CancelRequest만 APPLIED로 마킹
                cancelRequestTxService.markCancelRequestAsApplied(orderId);
                log.info("DLT: 유령 배송 방지 완료 (배송 없음): orderId={}", orderId);
            }

            // 9. ProcessedEvent 저장 (별도 트랜잭션)
            cancelRequestTxService.saveProcessedEvent(eventId, "PAYMENT_CANCELED_DLT");

            // 10. ACK
            ack.acknowledge();

            log.info("=== DLT 처리 완료: orderId={}, cancelled={}", orderId, cancelled);

        } catch (Exception e) {
            log.error("DLT 처리 중 예외: offset={}", record.offset(), e);
            // ACK 안 함 → Kafka 재시도
            throw new RuntimeException("DLT processing failed", e);
        }
    }
}

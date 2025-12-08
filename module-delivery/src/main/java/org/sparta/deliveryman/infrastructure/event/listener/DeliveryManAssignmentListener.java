package org.sparta.deliveryman.infrastructure.event.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.delivery.application.service.DeliveryService;
import org.sparta.delivery.domain.event.publisher.DeliveryCreatedEvent;
import org.sparta.delivery.presentation.dto.request.DeliveryRequest;
import org.sparta.deliveryman.application.service.DeliveryManService;
import org.sparta.deliveryman.domain.entity.DeliveryMan;
import org.sparta.deliveryman.domain.entity.ProcessedEvent;
import org.sparta.deliveryman.domain.repository.ProcessedEventRepository;
import org.sparta.deliverylog.application.service.DeliveryLogService;
import org.sparta.deliverylog.presentation.dto.request.DeliveryLogRequest;
import org.sparta.deliverylog.presentation.dto.response.DeliveryLogResponse;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * [DeliveryCreatedEvent 수신]
 * => DeliveryMan 배정 처리
 * => Delivery와 DeliveryLog에 배송 담당자 ID 저장
 *
 * 멱등성 보장:
 * - eventId 기반 중복 이벤트 체크
 * - 동일 이벤트 재처리 시 담당자 중복 배정 방지
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryManAssignmentListener {

    private final DeliveryService deliveryService;
    private final DeliveryManService deliveryManService;
    private final DeliveryLogService deliveryLogService;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "delivery-events",
            groupId = "deliveryman-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleDeliveryCreated(String message) {
        try {
            DeliveryCreatedEvent event = objectMapper.readValue(message, DeliveryCreatedEvent.class);
            log.info("Received DeliveryCreatedEvent: deliveryId={}, eventId={}, supplierHubId={}, receiveHubId={}",
                    event.deliveryId(), event.eventId(), event.sourceHubId(), event.targetHubId());

            // 멱등성 체크: eventId로 중복 이벤트 확인
            if (processedEventRepository.existsByEventId(event.eventId())) {
                log.info("Event already processed, skipping: eventId={}, deliveryId={}",
                        event.eventId(), event.deliveryId());
                return;
            }

            // 1. 허브 배송 담당자 배정
            DeliveryMan hubAssignedMan = deliveryManService.assignHubDeliveryMan();
            log.info("Hub DeliveryMan assigned: deliveryManId={}, sequence={}, deliveryCount={}",
                    hubAssignedMan.getId(), hubAssignedMan.getSequence(), hubAssignedMan.getDeliveryCount());

            // 2. Delivery에 허브 배송 담당자 배정
            //    - Delivery.hubDeliveryManId 저장
            //    - Delivery.status: CREATED -> HUB_WAITING
            DeliveryRequest.AssignHubDeliveryMan hubRequest =
                    new DeliveryRequest.AssignHubDeliveryMan(hubAssignedMan.getId());
            deliveryService.assignHubDeliveryMan(event.deliveryId(), hubRequest);
            log.info("Delivery updated with hub deliveryMan: deliveryId={}, hubDeliveryManId={}, status=HUB_WAITING",
                    event.deliveryId(), hubAssignedMan.getId());

            // 3. 모든 DeliveryLog에 허브 배송 담당자 배정
            assignHubDeliveryManToLogs(event.deliveryId(), hubAssignedMan.getId());

            // 이벤트 처리 완료 기록 (같은 트랜잭션)
            processedEventRepository.save(
                    ProcessedEvent.of(event.eventId(), "DELIVERY_CREATED")
            );

            log.info("DeliveryMan assignment completed successfully: deliveryId={}, hubDeliveryManId={}",
                    event.deliveryId(), hubAssignedMan.getId());

        } catch (Exception e) {
            log.error("Failed to handle delivery created event: {}", message, e);
            // 예외 발생 시 전체 트랜잭션 롤백 + Kafka 재처리
            throw new RuntimeException("DeliveryMan assignment failed", e);
        }
    }

    /**
     * 모든 DeliveryLog에 허브 배송 담당자 배정
     * - DeliveryLog.deliveryManId 저장
     * - DeliveryLog.status: CREATED -> HUB_WAITING
     */
    private void assignHubDeliveryManToLogs(UUID deliveryId, UUID hubDeliveryManId) {
        // Delivery의 모든 DeliveryLog 조회 (sequence 오름차순)
        List<DeliveryLogResponse.Summary> timeline =
                deliveryLogService.getTimelineByDeliveryId(deliveryId);

        // 타임라인이 없으면 예외 발생 (데이터 정합성 보장)
        if (timeline == null || timeline.isEmpty()) {
            log.error("No DeliveryLogs found for deliveryId={} when assigning hub deliveryMan. " +
                            "This indicates data inconsistency - 배송은 존재하지만 배송 로그가 존재하지 않습니다.",
                    deliveryId);
            throw new IllegalStateException(
                    "No DeliveryLogs found for deliveryId=" + deliveryId +
                            ". 배송 로그없이 허브 배송담당자를 배정할 수 없습니다."
            );
        }

        // 각 DeliveryLog에 허브 배송 담당자 배정
        for (DeliveryLogResponse.Summary logSummary : timeline) {
            DeliveryLogRequest.AssignDeliveryMan assignRequest =
                    new DeliveryLogRequest.AssignDeliveryMan(hubDeliveryManId);

            deliveryLogService.assignDeliveryMan(logSummary.id(), assignRequest);

            log.debug("DeliveryLog assigned: logId={}, sequence={}, deliveryManId={}, status=HUB_WAITING",
                    logSummary.id(), logSummary.sequence(), hubDeliveryManId);
        }

        log.info("All DeliveryLogs assigned to hub deliveryMan: deliveryId={}, logCount={}, deliveryManId={}",
                deliveryId, timeline.size(), hubDeliveryManId);
    }
}
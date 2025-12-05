package org.sparta.deliveryman.infrastructure.event.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.delivery.application.service.DeliveryService;
import org.sparta.delivery.domain.event.publisher.DeliveryCreatedEvent;
import org.sparta.delivery.presentation.dto.request.DeliveryRequest;
import org.sparta.deliveryman.application.service.DeliveryManService;
import org.sparta.deliveryman.domain.entity.DeliveryMan;
import org.sparta.deliverylog.application.service.DeliveryLogService;
import org.sparta.deliverylog.presentation.dto.request.DeliveryLogRequest;
import org.sparta.deliverylog.presentation.dto.response.DeliveryLogResponse;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * [DeliveryCreatedEvent 수신]
 * => DeliveryMan 배정 처리
 * => Delivery와 DeliveryLog에 배송 담당자 ID 저장
 * TODO : 현재는 해브배송 담당자와 업체 배송 답당자 배정/저장 시기를 분리해뒀음
 * TODO : 그래서 deliveryStatus가 COMPANY_WAITING 상태가 되면 업체 배송 담당자를 배정해서 저장하는 방식으로 조금 수정 해야 될듯
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryManAssignmentListener {

    private final DeliveryService deliveryService;
    private final DeliveryManService deliveryManService;
    private final DeliveryLogService deliveryLogService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "delivery-events",
            groupId = "deliveryman-assignment-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleDeliveryCreated(String message) {
        try {
            DeliveryCreatedEvent event = objectMapper.readValue(message, DeliveryCreatedEvent.class);
            log.info("Received DeliveryCreatedEvent: deliveryId={}, eventId={}, supplierHubId={}, receiveHubId={}",
                    event.deliveryId(), event.eventId(), event.sourceHubId(), event.targetHubId());

            // 1. 허브 배송 담당자 배정 (라운드 로빈)
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
            //    - DeliveryLog.deliveryManId 저장
            //    - DeliveryLog.status: CREATED -> HUB_WAITING
            assignHubDeliveryManToLogs(event.deliveryId(), hubAssignedMan.getId());

            log.info("DeliveryMan assignment completed successfully: deliveryId={}, hubDeliveryManId={}",
                    event.deliveryId(), hubAssignedMan.getId());

        } catch (Exception e) {
            log.error("Failed to handle delivery created event: {}", message, e);
            // TODO: DLQ 처리 추가 예정
            // 배정 실패 시 롤백 처리도 고려 필요
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

        if (timeline == null || timeline.isEmpty()) {
            log.warn("No DeliveryLogs found for deliveryId={}", deliveryId);
            return;
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

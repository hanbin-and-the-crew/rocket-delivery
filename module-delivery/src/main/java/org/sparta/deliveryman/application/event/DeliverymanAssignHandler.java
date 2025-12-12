package org.sparta.deliveryman.application.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.delivery.application.service.DeliveryService;
import org.sparta.delivery.domain.event.publisher.DeliveryCreatedEvent;
import org.sparta.delivery.domain.entity.Delivery;
import org.sparta.delivery.domain.repository.DeliveryRepository;
import org.sparta.delivery.presentation.dto.request.DeliveryRequest;
import org.sparta.deliverylog.application.service.DeliveryLogService;
import org.sparta.deliverylog.presentation.dto.request.DeliveryLogRequest;
import org.sparta.deliverylog.presentation.dto.response.DeliveryLogResponse;
import org.sparta.deliveryman.application.service.DeliveryManService;
import org.sparta.deliveryman.domain.entity.DeliveryMan;
import org.sparta.deliveryman.domain.entity.ProcessedEvent;
import org.sparta.deliveryman.domain.repository.DeliveryManRepository;
import org.sparta.deliveryman.domain.repository.ProcessedEventRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliverymanAssignHandler {

    private final DeliveryService deliveryService;
    private final DeliveryManService deliveryManService;
    private final DeliveryLogService deliveryLogService;
    private final ProcessedEventRepository processedEventRepository;
    private final DeliveryRepository deliveryRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onDeliveryCreated(DeliveryCreatedEvent event) {
        log.info("tx active? {}", TransactionSynchronizationManager.isActualTransactionActive());

        try {
            log.info("tx active? {}", TransactionSynchronizationManager.isActualTransactionActive());

            // 멱등성 체크: eventId로 중복 이벤트 확인
            if (processedEventRepository.existsByEventId(event.eventId())) {
                log.info("Event already processed, skipping: eventId={}, deliveryId={}",
                        event.eventId(), event.deliveryId());
                return;
            }

            // 1) 이벤트에서 deliveryId 꺼내기
            UUID deliveryId = event.deliveryId();

            // 2) 해당 Delivery 조회
            Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                    .orElseThrow(() -> new IllegalStateException("Delivery not found: " + deliveryId));

            // 1. 허브 배송 담당자 배정
            DeliveryMan hubAssignedMan = deliveryManService.assignHubDeliveryMan();
            log.info("Hub DeliveryMan assigned: deliveryManId={}, sequence={}, deliveryCount={}",
                    hubAssignedMan.getId(), hubAssignedMan.getSequence(), hubAssignedMan.getDeliveryCount());

            // 2. Delivery에 허브 배송 담당자 배정
            DeliveryRequest.AssignHubDeliveryMan hubRequest =
                    new DeliveryRequest.AssignHubDeliveryMan(hubAssignedMan.getId());
            deliveryService.assignHubDeliveryMan(event.deliveryId(), hubRequest);
            log.info("Delivery updated with hub deliveryMan: deliveryId={}, hubDeliveryManId={}, status=HUB_WAITING",
                    event.deliveryId(), hubAssignedMan.getId());

            // 3. 모든 DeliveryLog에 허브 배송 담당자 배정
            assignHubDeliveryManToLogs(event.deliveryId(), hubAssignedMan.getId());

            // 이벤트 처리 완료 기록
            processedEventRepository.save(
                    ProcessedEvent.of(event.eventId(), "DELIVERY_CREATED")
            );

            log.info("DeliveryMan assignment completed successfully: deliveryId={}, hubDeliveryManId={}",
                    event.deliveryId(), hubAssignedMan.getId());

            // JPA이면 트랜잭션 안에서 dirty checking으로 flush됨

        } catch (Exception e) {
            log.error("Failed to handle delivery created event: ", e);
            throw new RuntimeException("DeliveryMan assignment failed", e);
        }
    }

    /**
     * //     * 모든 DeliveryLog에 허브 배송 담당자 배정
     * //
     */
    private void assignHubDeliveryManToLogs(UUID deliveryId, UUID hubDeliveryManId) {
        List<DeliveryLogResponse.Summary> timeline =
                deliveryLogService.getTimelineByDeliveryId(deliveryId);

        if (timeline == null || timeline.isEmpty()) {
            log.error("No DeliveryLogs found for deliveryId={} when assigning hub deliveryMan. " +
                            "This indicates data inconsistency - 배송은 존재하지만 배송 로그가 존재하지 않습니다.",
                    deliveryId);
            throw new IllegalStateException(
                    "No DeliveryLogs found for deliveryId=" + deliveryId +
                            ". 배송 로그없이 허브 배송담당자를 배정할 수 없습니다."
            );
        }

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

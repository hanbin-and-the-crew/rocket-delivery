package org.sparta.deliveryman.application.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.delivery.application.service.DeliveryService;
import org.sparta.delivery.infrastructure.event.publisher.DeliveryLastHubArrivedEvent;
import org.sparta.delivery.presentation.dto.request.DeliveryRequest;
import org.sparta.deliveryman.application.service.DeliveryManService;
import org.sparta.deliveryman.domain.entity.DeliveryMan;
import org.sparta.deliveryman.domain.entity.ProcessedEvent;
import org.sparta.deliveryman.domain.repository.ProcessedEventRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [DeliveryLastHubArrivedEvent 수신]
 * => 마지막 허브 도착 시 업체 배송 담당자 배정
 * => Delivery.companyDeliveryManId 저장
 * => Delivery.status: DEST_HUB_ARRIVED 상태에서 업체 담당자 배정 가능
 *
 * 멱등성 보장:
 * - eventId 기반 중복 이벤트 체크
 * - 동일 이벤트 재처리 시 담당자 중복 배정 방지
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryManCompanyAssignmentListener {

    private final DeliveryService deliveryService;
    private final DeliveryManService deliveryManService;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "delivery-events",
            groupId = "deliveryman-company-assignment-group"
    )
    @Transactional
    public void handleLastHubArrived(String message) {
        try {
            DeliveryLastHubArrivedEvent event = objectMapper.readValue(message, DeliveryLastHubArrivedEvent.class);
            log.info("Received DeliveryLastHubArrivedEvent: deliveryId={}, eventId={}, receiveHubId={}",
                    event.deliveryId(), event.eventId(), event.receiveHubId());

            // 멱등성 체크: eventId로 중복 이벤트 확인
            if (processedEventRepository.existsByEventId(event.eventId())) {
                log.info("Event already processed, skipping: eventId={}, deliveryId={}",
                        event.eventId(), event.deliveryId());
                return;
            }

            // 1. 업체 배송 담당자 배정 (라운드 로빈 - 해당 허브 소속)
            DeliveryMan companyAssignedMan = deliveryManService.assignCompanyDeliveryMan(event.receiveHubId());
            log.info("Company DeliveryMan assigned: deliveryManId={}, hubId={}, sequence={}, deliveryCount={}",
                    companyAssignedMan.getId(),
                    companyAssignedMan.getHubId(),
                    companyAssignedMan.getSequence(),
                    companyAssignedMan.getDeliveryCount());

            // 2. Delivery에 업체 배송 담당자 배정
            //    - Delivery.companyDeliveryManId 저장
            //    - Delivery.status: DEST_HUB_ARRIVED 유지 (업체 출발 대기)
            DeliveryRequest.AssignCompanyDeliveryMan companyRequest =
                    new DeliveryRequest.AssignCompanyDeliveryMan(companyAssignedMan.getId());
            deliveryService.assignCompanyDeliveryMan(event.deliveryId(), companyRequest);
            log.info("Delivery updated with company deliveryMan: deliveryId={}, companyDeliveryManId={}, status=DEST_HUB_ARRIVED",
                    event.deliveryId(), companyAssignedMan.getId());

            // 이벤트 처리 완료 기록 (같은 트랜잭션)
            processedEventRepository.save(
                    ProcessedEvent.of(event.eventId(), "LAST_HUB_ARRIVED")
            );

            log.info("Company DeliveryMan assignment completed successfully: deliveryId={}, companyDeliveryManId={}",
                    event.deliveryId(), companyAssignedMan.getId());

        } catch (Exception e) {
            log.error("Failed to handle last hub arrived event: {}", message, e);
            // 예외 발생 시 전체 트랜잭션 롤백 + Kafka 재시도 필요
            throw new RuntimeException("Company DeliveryMan assignment failed", e);
        }
    }
}
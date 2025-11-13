package org.sparta.deliveryman.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.error.BusinessException;
import org.sparta.deliveryman.application.dto.DeliveryManRequest;
import org.sparta.deliveryman.application.dto.DeliveryManResponse;
import org.sparta.deliveryman.domain.entity.DeliveryMan;
import org.sparta.deliveryman.domain.event.*;
import org.sparta.deliveryman.domain.enumeration.DeliveryManStatus;
import org.sparta.deliveryman.domain.enumeration.DeliveryManType;
import org.sparta.deliveryman.domain.repository.DeliveryManRepository;
import org.sparta.deliveryman.exception.DeliveryManErrorType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryManService {

    private final DeliveryManRepository deliveryManRepository;
    private final DeliveryManEventPublisher eventPublisher;

    @Transactional
    public DeliveryManResponse.Detail createDeliveryMan(DeliveryManRequest.Create request) {
        DeliveryMan deliveryMan = DeliveryMan.create(
                request.userId(),
                request.userName(),
                request.email(),
                request.phoneNumber(),
                request.affiliationHubId(),
                request.slackId(),
                DeliveryManType.valueOf(request.deliveryManType()),
                DeliveryManStatus.WAITING
        );
        deliveryManRepository.save(deliveryMan);
        eventPublisher.publishCreatedEvent(deliveryMan.toCreatedEvent());
        log.info("배송 담당자 생성: {}", deliveryMan.getId());
        return DeliveryManResponse.Detail.fromEntity(deliveryMan);
    }

    public DeliveryManResponse.Detail getDeliveryMan(UUID id) {
        DeliveryMan deliveryMan = findDeliveryManById(id);
        return DeliveryManResponse.Detail.fromEntity(deliveryMan);
    }

    public Page<DeliveryManResponse.Summary> getAllDeliveryMen(Pageable pageable) {
        Page<DeliveryMan> deliveryMen = deliveryManRepository.findAllActive(pageable);
        return deliveryMen.map(DeliveryManResponse.Summary::new);
    }

    @Transactional
    public DeliveryManResponse.Detail updateDeliveryMan(UUID id, DeliveryManRequest.Update request) {
        DeliveryMan deliveryMan = findDeliveryManById(id);
        deliveryMan.update(
                request.userName(),
                request.email(),
                request.phoneNumber(),
                request.affiliationHubId(),
                request.slackId(),
                DeliveryManType.valueOf(request.deliveryManType()),
                DeliveryManStatus.valueOf(request.status())
        );
        deliveryManRepository.save(deliveryMan);
        eventPublisher.publishUpdatedEvent(deliveryMan.toUpdatedEvent());
        log.info("배송 담당자 업데이트: {}", deliveryMan.getId());
        return DeliveryManResponse.Detail.fromEntity(deliveryMan);
    }

    @Transactional
    public void deleteDeliveryMan(UUID id) {
        DeliveryMan deliveryMan = findDeliveryManById(id);
        deliveryMan.delete();
        deliveryManRepository.save(deliveryMan);
        eventPublisher.publishDeletedEvent(deliveryMan.toDeletedEvent());
        log.info("배송 담당자 삭제: {}", deliveryMan.getId());
    }

    // 허브 배송 담당자 지정 (배송 생성 완료시)
    @Transactional
    public void assignHubDeliveryManagerToDelivery(UUID deliveryId, UUID hubId) {
        DeliveryMan manager = findAvailableDeliveryManager(hubId, DeliveryManType.HUB_DELIVERY_MAN, DeliveryManStatus.WAITING);
        manager.incrementAssignedDeliveryCount();
        deliveryManRepository.save(manager);

        eventPublisher.publishHubDeliveryManagerAssignedEvent(
                HubDeliveryManagerAssignedEvent.of(deliveryId, manager)
        );
    }

    // 업체 배송 담당자 지정 (마지막 목적지 허브 도착시)
    @Transactional
    public void assignPartnerDeliveryManagerToDelivery(UUID deliveryId, UUID hubId) {
        DeliveryMan manager = findAvailableDeliveryManager(hubId, DeliveryManType.COMPANY_DELIVERY_MAN, DeliveryManStatus.WAITING);
        manager.incrementAssignedDeliveryCount();
        deliveryManRepository.save(manager);

        eventPublisher.publishPartnerDeliveryManagerAssignedEvent(
                PartnerDeliveryManagerAssignedEvent.of(deliveryId, manager)
        );
    }

    // 지정 담당자 찾는 유틸
    public DeliveryMan findAvailableDeliveryManager(UUID hubId, DeliveryManType type, DeliveryManStatus status) {
        return deliveryManRepository
                .findByAffiliationHubIdAndDeliveryManTypeAndStatusOrderByAssignedDeliveryCountAsc(hubId, type, status)
                .stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(DeliveryManErrorType.DELIVERY_MAN_NOT_FOUND));
    }

    private DeliveryMan findDeliveryManById(UUID id) {
        return deliveryManRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new BusinessException(DeliveryManErrorType.DELIVERY_MAN_NOT_FOUND));
    }
}

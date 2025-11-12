package org.sparta.delivery.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.error.BusinessException;
import org.sparta.delivery.application.dto.request.DeliveryRequest;
import org.sparta.delivery.application.dto.response.DeliveryResponse;
import org.sparta.delivery.domain.entity.Delivery;
import org.sparta.delivery.domain.error.DeliveryErrorType;
import org.sparta.delivery.infrastructure.repository.DeliveryJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryService {

    private final DeliveryJpaRepository deliveryRepository;

    @Transactional
    public DeliveryResponse.Create createDelivery(DeliveryRequest.Create request, UUID userId) {
        log.info("배송 생성 시작 - orderId: {}, userId: {}", request.orderId(), userId);

        // 중복 체크
        deliveryRepository.findByOrderIdAndDeletedAtIsNull(request.orderId())
                .ifPresent(d -> {
                    throw new BusinessException(DeliveryErrorType.DELIVERY_ALREADY_EXISTS);
                });

        Delivery delivery = Delivery.create(
                request.orderId(),
                request.departureHubId(),
                request.destinationHubId(),
                request.deliveryAddress(),
                request.recipientName(),
                request.recipientSlackId()
        );

        Delivery savedDelivery = deliveryRepository.save(delivery);

        log.info("배송 생성 완료 - deliveryId: {}", savedDelivery.getId());
        return DeliveryResponse.Create.of(savedDelivery);
    }

    public DeliveryResponse.Detail getDelivery(UUID deliveryId, UUID userId) {
        log.info("배송 조회 - deliveryId: {}, userId: {}", deliveryId, userId);

        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BusinessException(DeliveryErrorType.DELIVERY_NOT_FOUND));

        return DeliveryResponse.Detail.of(delivery);
    }

    public List<DeliveryResponse.Summary> getAllDeliveries(UUID userId) {
        log.info("배송 목록 조회 - userId: {}", userId);

        return deliveryRepository.findAll().stream()
                .map(DeliveryResponse.Summary::of)
                .toList();
    }

    @Transactional
    public DeliveryResponse.Detail updateAddress(UUID deliveryId, DeliveryRequest.UpdateAddress request, UUID userId) {
        log.info("배송지 주소 변경 - deliveryId: {}, userId: {}", deliveryId, userId);

        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BusinessException(DeliveryErrorType.DELIVERY_NOT_FOUND));

        delivery.updateAddress(request.deliveryAddress());

        return DeliveryResponse.Detail.of(delivery);
    }

    @Transactional
    public DeliveryResponse.Detail assignDeliveryMan(UUID deliveryId, DeliveryRequest.AssignDeliveryMan request, UUID userId) {
        log.info("배송 담당자 배정 - deliveryId: {}, companyDeliveryManId: {}, hubDeliveryManId: {}, userId: {}",
                deliveryId, request.companyDeliveryManId(), request.hubDeliveryManId(), userId);

        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BusinessException(DeliveryErrorType.DELIVERY_NOT_FOUND));

        delivery.saveDeliveryMan(request.companyDeliveryManId(), request.hubDeliveryManId());

        return DeliveryResponse.Detail.of(delivery);
    }

    @Transactional
    public DeliveryResponse.Detail hubWaiting(UUID deliveryId, UUID userId) {
        log.info("허브 대기 - deliveryId: {}, userId: {}", deliveryId, userId);

        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BusinessException(DeliveryErrorType.DELIVERY_NOT_FOUND));

        delivery.hubWaiting();

        return DeliveryResponse.Detail.of(delivery);
    }

    @Transactional
    public DeliveryResponse.Detail hubMoving(UUID deliveryId, UUID userId) {
        log.info("허브 이동 - deliveryId: {}, userId: {}", deliveryId, userId);

        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BusinessException(DeliveryErrorType.DELIVERY_NOT_FOUND));

        delivery.hubMoving();

        return DeliveryResponse.Detail.of(delivery);
    }

    @Transactional
    public DeliveryResponse.Detail arriveAtDestinationHub(UUID deliveryId, UUID userId) {
        log.info("목적지 허브 도착 - deliveryId: {}, userId: {}", deliveryId, userId);

        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BusinessException(DeliveryErrorType.DELIVERY_NOT_FOUND));

        delivery.arriveAtDestinationHub();

        return DeliveryResponse.Detail.of(delivery);
    }

    @Transactional
    public DeliveryResponse.Detail startCompanyMoving(UUID deliveryId, DeliveryRequest.StartCompanyMoving request, UUID userId) {
        log.info("업체 이동 시작 - deliveryId: {}, companyDeliveryManId: {}, userId: {}",
                deliveryId, request.companyDeliveryManId(), userId);

        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BusinessException(DeliveryErrorType.DELIVERY_NOT_FOUND));

        delivery.startCompanyMoving(request.companyDeliveryManId());

        return DeliveryResponse.Detail.of(delivery);
    }

    @Transactional
    public DeliveryResponse.Detail completeDelivery(UUID deliveryId, UUID userId) {
        log.info("배송 완료 - deliveryId: {}, userId: {}", deliveryId, userId);

        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BusinessException(DeliveryErrorType.DELIVERY_NOT_FOUND));

        delivery.completeDelivery();

        return DeliveryResponse.Detail.of(delivery);
    }

    @Transactional
    public void deleteDelivery(UUID deliveryId, UUID userId) {
        log.info("배송 삭제 - deliveryId: {}, userId: {}", deliveryId, userId);

        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new BusinessException(DeliveryErrorType.DELIVERY_NOT_FOUND));

        delivery.delete(userId);
    }
}

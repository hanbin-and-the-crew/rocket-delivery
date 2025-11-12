package org.sparta.delivery.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.error.BusinessException;
import org.sparta.delivery.application.dto.request.DeliveryRequest;
import org.sparta.delivery.application.dto.response.DeliveryResponse;
import org.sparta.delivery.domain.entity.Delivery;
import org.sparta.delivery.domain.error.DeliveryErrorType;
import org.sparta.delivery.infrastructure.repository.DeliveryJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryService {

    private final DeliveryJpaRepository deliveryRepository;

    private static final List<Integer> ALLOWED_PAGE_SIZES = Arrays.asList(10, 30, 50);
    private static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * 배송 생성
     */
    @Transactional
    public DeliveryResponse.Create createDelivery(DeliveryRequest.Create request, UUID userId) {
        log.info("배송 생성 - orderId: {}, userId: {}", request.orderId(), userId);

        // 중복 체크
        deliveryRepository.findByOrderIdAndDeletedAtIsNull(request.orderId())
                .ifPresent(delivery -> {
                    throw new BusinessException(DeliveryErrorType.DELIVERY_ALREADY_EXISTS);
                });

        // 배송 생성
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

        return DeliveryResponse.Create.from(savedDelivery);
    }

    /**
     * 배송 목록 조회 (페이징 + 정렬)
     */
    @Transactional(readOnly = true)
    public Page<DeliveryResponse.Summary> getAllDeliveries(Pageable pageable) {
        Page<Delivery> deliveries = deliveryRepository.findAllNotDeleted(pageable);
        return deliveries.map(DeliveryResponse.Summary::from);
    }

    /**
     * 배송 상세 조회
     */
    public DeliveryResponse.Detail getDelivery(UUID deliveryId, UUID userId) {
        log.info("배송 상세 조회 - deliveryId: {}, userId: {}", deliveryId, userId);

        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BusinessException(DeliveryErrorType.DELIVERY_NOT_FOUND));

        return DeliveryResponse.Detail.from(delivery);
    }

    /**
     * 배송 상태 변경
     */
    @Transactional
    public DeliveryResponse.Update updateStatus(
            UUID deliveryId,
            DeliveryRequest.UpdateStatus request,
            UUID userId
    ) {
        log.info("배송 상태 변경 - deliveryId: {}, status: {}", deliveryId, request.deliveryStatus());

        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BusinessException(DeliveryErrorType.DELIVERY_NOT_FOUND));

        delivery.updateStatus(request.deliveryStatus());

        return DeliveryResponse.Update.from(delivery);
    }

    /**
     * 배송 주소 변경
     */
    @Transactional
    public DeliveryResponse.Update updateAddress(
            UUID deliveryId,
            DeliveryRequest.UpdateAddress request,
            UUID userId
    ) {
        log.info("배송 주소 변경 - deliveryId: {}, address: {}", deliveryId, request.deliveryAddress());

        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BusinessException(DeliveryErrorType.DELIVERY_NOT_FOUND));

        delivery.updateAddress(request.deliveryAddress());

        return DeliveryResponse.Update.from(delivery);
    }

    /**
     * 배송 담당자 배정
     */
    @Transactional
    public DeliveryResponse.Update assignDeliveryMan(
            UUID deliveryId,
            DeliveryRequest.AssignDeliveryMan request,
            UUID userId
    ) {
        log.info("배송 담당자 배정 - deliveryId: {}, companyManId: {}, hubManId: {}",
                deliveryId, request.companyDeliveryManId(), request.hubDeliveryManId());

        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BusinessException(DeliveryErrorType.DELIVERY_NOT_FOUND));

        delivery.saveDeliveryMan(request.companyDeliveryManId(), request.hubDeliveryManId());

        return DeliveryResponse.Update.from(delivery);
    }

    /**
     * 업체 배송 시작
     */
    @Transactional
    public DeliveryResponse.Update startCompanyMoving(UUID deliveryId, DeliveryRequest.StartCompanyMoving request, UUID userId) {
        log.info("업체 배송 시작 - deliveryId: {}, companyManId: {}", deliveryId, request.companyDeliveryManId());

        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BusinessException(DeliveryErrorType.DELIVERY_NOT_FOUND));

        delivery.startCompanyMoving(request.companyDeliveryManId());

        return DeliveryResponse.Update.from(delivery);
    }

    /**
     * 배송 완료
     */
    @Transactional
    public DeliveryResponse.Update completeDelivery(UUID deliveryId, UUID userId) {
        log.info("배송 완료 - deliveryId: {}, userId: {}", deliveryId, userId);

        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BusinessException(DeliveryErrorType.DELIVERY_NOT_FOUND));

        delivery.completeDelivery();

        return DeliveryResponse.Update.from(delivery);
    }

    /**
     * 배송 삭제 (논리 삭제)
     */
    @Transactional
    public DeliveryResponse.Delete deleteDelivery(UUID deliveryId, UUID userId) {
        log.info("배송 삭제 - deliveryId: {}, userId: {}", deliveryId, userId);

        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BusinessException(DeliveryErrorType.DELIVERY_NOT_FOUND));

        delivery.delete(userId);

        return DeliveryResponse.Delete.from(delivery);
    }

    /**
     * 페이지 크기를 검증하고 정렬을 조정합니다.
     */
    private Pageable validateAndAdjustPageable(Pageable pageable) {
        int pageSize = pageable.getPageSize();

        // 허용되지 않은 크기는 기본값(10)으로 설정
        if (!ALLOWED_PAGE_SIZES.contains(pageSize)) {
            pageSize = DEFAULT_PAGE_SIZE;
        }

        // 정렬 조정: 동률 처리를 위해 createdAt 추가
        Sort sort = adjustSortWithCreatedAt(pageable.getSort());

        return PageRequest.of(pageable.getPageNumber(), pageSize, sort);
    }

    /**
     * 정렬에 생성일자를 추가하여 동률을 처리합니다.
     */
    private Sort adjustSortWithCreatedAt(Sort sort) {
        if (sort.isEmpty()) {
            // 정렬이 없으면 생성일순 DESC
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        // 이미 createdAt이 포함되어 있는지 확인
        boolean hasCreatedAt = false;
        for (Sort.Order order : sort) {
            if ("createdAt".equals(order.getProperty())) {
                hasCreatedAt = true;
                break;
            }
        }

        // createdAt이 없으면 추가 (동률 처리용)
        if (!hasCreatedAt) {
            return sort.and(Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        return sort;
    }
}

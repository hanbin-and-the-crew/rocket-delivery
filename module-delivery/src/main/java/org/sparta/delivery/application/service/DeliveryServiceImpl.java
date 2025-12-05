package org.sparta.delivery.application.service;

import ch.qos.logback.classic.Logger;
import lombok.RequiredArgsConstructor;
import org.sparta.common.error.BusinessException;
import org.sparta.common.event.EventPublisher;
import org.sparta.delivery.domain.entity.Delivery;
import org.sparta.delivery.domain.enumeration.DeliveryStatus;
import org.sparta.delivery.domain.error.DeliveryErrorType;
import org.sparta.delivery.infrastructure.event.publisher.DeliveryCompletedEvent;
import org.sparta.delivery.domain.event.publisher.DeliveryCreatedEvent;
import org.sparta.delivery.infrastructure.event.publisher.DeliveryStartedEvent;
import org.sparta.delivery.domain.repository.DeliveryRepository;
import org.sparta.delivery.infrastructure.event.OrderApprovedEvent;
import org.sparta.delivery.presentation.dto.request.DeliveryRequest;
import org.sparta.delivery.presentation.dto.response.DeliveryResponse;
import org.sparta.deliverylog.application.service.DeliveryLogService;
import org.sparta.deliverylog.presentation.dto.request.DeliveryLogRequest;
import org.sparta.deliverylog.presentation.dto.response.DeliveryLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sparta.delivery.infrastructure.client.HubRouteFeignClient;
import org.sparta.delivery.presentation.dto.response.HubLegResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryLogService deliveryLogService;
    private final HubRouteFeignClient hubRouteFeignClient; // 허브 경로 조회용 Feign
    private final EventPublisher eventPublisher;
    Logger log;

    // ================================
    // 1. 배송 생성 - 단순(테스트용) /TODO: 허브 경로 조회 기능 추가해서 실제 자동으로 실행되는 api와 동일하게 동작하게 하기
    // ================================
    @Override
    @Transactional
    public DeliveryResponse.Detail createSimple(DeliveryRequest.Create request) {

        // 주문당 하나만 허용하고 싶다면 이 검증 사용
        if (deliveryRepository.existsByOrderIdAndDeletedAtIsNull(request.orderId())) {
            throw new BusinessException(DeliveryErrorType.DELIVERY_ALREADY_DELETED);
        }

        Delivery delivery = Delivery.createFromOrderApproved(
                request.orderId(),
                request.customerId(),
                request.supplierCompanyId(),
                request.supplierHubId(),
                request.receiveCompanyId(),
                request.receiveHubId(),
                request.address(),
                request.receiverName(),
                request.receiverSlackId(),
                request.receiverPhone(),
                request.dueAt(),
                request.requestedMemo(),
                request.totalLogSeq()
        );

        Delivery saved = deliveryRepository.save(delivery);
        return DeliveryResponse.Detail.from(saved);
    }

    // ================================
    // 2. 배송 생성 - 허브 경로 + 로그 생성까지
    // ================================
    @Override
    @Transactional
    public DeliveryResponse.Detail createWithRoute(OrderApprovedEvent orderEvent) {
        try {

        // 1) 허브 서비스에서 경로/leg 정보 조회 (Feign)
        //    - 예시: List<HubLegResponse> legs = hubRouteFeignClient.getRouteLegs(...);
        List<HubLegResponse> legs = hubRouteFeignClient.getRouteLegs(
                orderEvent.supplierHubId(),
                orderEvent.receiveHubId()
        );

        if (legs == null || legs.isEmpty()) {
            throw new BusinessException(DeliveryErrorType.INVALID_TOTAL_LOG_SEQ);
            // TODO : Delivery 생성 실패 Event 발행
        }

        // 2) Delivery 생성 (위 메서드와 동일한 검증/로직)
        Delivery delivery = Delivery.createFromOrderApproved(
                orderEvent.orderId(),
                orderEvent.customerId(),
                orderEvent.supplierCompanyId(),
                orderEvent.supplierHubId(),
                orderEvent.receiveCompanyId(),
                orderEvent.receiveHubId(),
                orderEvent.address(),
                orderEvent.receiverName(),
                orderEvent.receiverSlackId(),
                orderEvent.receiverPhone(),
                orderEvent.dueAt(),
                orderEvent.requestMemo(),
                null // totalLogSeq는 경로 계산 후 세팅
        );

        Delivery savedDelivery = deliveryRepository.save(delivery);

        int sequence = 0;
        for (HubLegResponse leg : legs) {
            DeliveryLogRequest.Create logCreate = new DeliveryLogRequest.Create(
                    savedDelivery.getId(),
                    sequence,
                    leg.sourceHubId(),
                    leg.targetHubId(),
                    leg.estimatedKm(),
                    leg.estimatedMinutes()
            );
            deliveryLogService.create(logCreate);
            sequence++;
        }

        // 3) 전체 leg 개수 세팅
        savedDelivery.updateTotalLogSeq(sequence);

        // [배송 생성 완료 이벤트 발행]
        eventPublisher.publishExternal(new DeliveryCreatedEvent(
                UUID.randomUUID(),
                savedDelivery.getOrderId(),
                savedDelivery.getId(),
                savedDelivery.getSupplierHubId(),
                savedDelivery.getReceiveHubId(),
                savedDelivery.getTotalLogSeq(),
                Instant.now()
        ));

        return DeliveryResponse.Detail.from(savedDelivery);
        } catch (Exception e) {
            log.error("Failed to create delivery from order: {}", orderEvent.orderId(), e);
            throw new BusinessException(DeliveryErrorType.CREATION_FAILED);
        }
    }

    // ================================
    // 3. 허브 배송 담당자 배정
    // ================================
    @Override
    @Transactional
    public DeliveryResponse.Detail assignHubDeliveryMan(UUID deliveryId, DeliveryRequest.AssignHubDeliveryMan request) {
        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BusinessException(DeliveryErrorType.DELIVERY_NOT_FOUND));

        delivery.assignHubDeliveryMan(request.hubDeliveryManId());
        return DeliveryResponse.Detail.from(delivery);
    }

    // ================================
    // 4. 업체 배송 담당자 배정
    // ================================
    @Override
    @Transactional
    public DeliveryResponse.Detail assignCompanyDeliveryMan(UUID deliveryId, DeliveryRequest.AssignCompanyDeliveryMan request) {
        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BusinessException(DeliveryErrorType.DELIVERY_NOT_FOUND));

        delivery.assignCompanyDeliveryMan(request.companyDeliveryManId());
        return DeliveryResponse.Detail.from(delivery);
    }

    // ================================
    // 5. 허브 leg 출발
    // ================================
    @Override
    @Transactional
    public DeliveryResponse.Detail startHubMoving(UUID deliveryId, DeliveryRequest.StartHubMoving request) {
        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BusinessException(DeliveryErrorType.DELIVERY_NOT_FOUND));

        int seq = request.sequence();

        // Delivery 상태 변경 (HUB_WAITING -> HUB_MOVING, currentLogSeq 세팅)
        delivery.startHubMoving(seq);

        // DeliveryLog 쪽 상태 전이: HUB_WAITING -> HUB_MOVING
        // log ID를 알기 위해서는 DeliveryId + seq로 조회하는 로직이 필요하지만,
        // 지금 구조에서는 DeliveryLogService가 ID 기반이라서
        // "해당 시퀀스를 가진 로그 ID를 조회하는" 별도 메서드가 필요하다.
        // 여기서는 예시로, 타임라인 전체를 받아서 해당 seq의 log를 찾는 방식으로 처리한다.
        List<DeliveryLogResponse.Summary> timeline = deliveryLogService.getTimelineByDeliveryId(deliveryId);
        DeliveryLogResponse.Summary target = timeline.stream()
                .filter(log -> log.sequence() == seq)
                .findFirst()
                .orElseThrow(() -> new BusinessException(DeliveryErrorType.INVALID_LOG_SEQUENCE));

        deliveryLogService.startLog(target.id());

        // [ 배송 시작 이벤트 ] _ 첫 허브를 출발할 때만 이벤트 발행
        if (request.sequence() == 0) {
            eventPublisher.publishExternal(new DeliveryStartedEvent(
                    UUID.randomUUID(),
                    delivery.getOrderId(),
                    delivery.getId(),
                    delivery.getSupplierHubId(),
                    Instant.now()
            ));
        }

        return DeliveryResponse.Detail.from(delivery);
    }

    // ================================
    // 6. 허브 leg 도착
    // ================================
    @Override
    @Transactional
    public DeliveryResponse.Detail completeHubMoving(UUID deliveryId, DeliveryRequest.CompleteHubMoving request) {
        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BusinessException(DeliveryErrorType.DELIVERY_NOT_FOUND));

        int seq = request.sequence();

        // 전체 leg 개수 기준으로 마지막 leg인지 판단
        Integer total = delivery.getTotalLogSeq();
        if (total == null || total <= 0) {
            throw new BusinessException(DeliveryErrorType.INVALID_TOTAL_LOG_SEQ);
        }
        boolean isLastLog = (seq == total - 1);

        // Delivery 상태 전이 (HUB_MOVING -> HUB_WAITING / DEST_HUB_ARRIVED)
        delivery.completeHubMoving(seq, isLastLog);

        // DeliveryLog 상태 전이 (HUB_MOVING -> HUB_ARRIVED + 실제 거리/시간 기록)
        List<DeliveryLogResponse.Summary> timeline = deliveryLogService.getTimelineByDeliveryId(deliveryId);
        DeliveryLogResponse.Summary target = timeline.stream()
                .filter(log -> log.sequence() == seq)
                .findFirst()
                .orElseThrow(() -> new BusinessException(DeliveryErrorType.INVALID_LOG_SEQUENCE));

        DeliveryLogRequest.Arrive arriveReq = new DeliveryLogRequest.Arrive(
                request.actualKm(),
                request.actualMinutes()
        );
        deliveryLogService.arriveLog(target.id(), arriveReq);

        return DeliveryResponse.Detail.from(delivery);
    }

    // ================================
    // 7. 업체 구간 시작/완료
    // ================================
    @Override
    @Transactional
    public DeliveryResponse.Detail startCompanyMoving(UUID deliveryId) {
        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BusinessException(DeliveryErrorType.DELIVERY_NOT_FOUND));

        delivery.startCompanyMoving();
        return DeliveryResponse.Detail.from(delivery);
    }

    @Override
    @Transactional
    public DeliveryResponse.Detail completeDelivery(UUID deliveryId) {
        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BusinessException(DeliveryErrorType.DELIVERY_NOT_FOUND));

        delivery.completeDelivery();

        // [ 배송 최종 완료 이벤트 ]
        eventPublisher.publishExternal(new DeliveryCompletedEvent(
                UUID.randomUUID(),
                delivery.getOrderId(),
                delivery.getId(),
                delivery.getReceiveCompanyId(),
                Instant.now()
        ));

        return DeliveryResponse.Detail.from(delivery);
    }

    // ================================
    // 8. 취소/삭제
    // ================================
    @Override
    @Transactional
    public DeliveryResponse.Detail cancel(UUID deliveryId) {
        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BusinessException(DeliveryErrorType.DELIVERY_NOT_FOUND));

        if (delivery.getStatus() != DeliveryStatus.CREATED &&
                delivery.getStatus() != DeliveryStatus.HUB_WAITING) {
            throw new BusinessException(DeliveryErrorType.INVALID_STATUS_FOR_CANCEL);
        }

        delivery.cancel();
        return DeliveryResponse.Detail.from(delivery);
    }

    @Override
    @Transactional
    public void delete(UUID deliveryId) {
        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BusinessException(DeliveryErrorType.DELIVERY_NOT_FOUND));

        if (delivery.getDeletedAt() != null) {
            throw new BusinessException(DeliveryErrorType.DELIVERY_ALREADY_DELETED);
        }

        delivery.delete();
    }

    // ================================
    // 9. 조회
    // ================================
    @Override
    public DeliveryResponse.Detail getDetail(UUID deliveryId) {
        Delivery delivery = deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BusinessException(DeliveryErrorType.DELIVERY_NOT_FOUND));

        return DeliveryResponse.Detail.from(delivery);
    }

    @Override
    public DeliveryResponse.PageResult search(DeliveryRequest.Search request, Pageable pageable) {
        Sort.Direction direction = parseDirection(request.sortDirection());

        Page<Delivery> page = deliveryRepository.search(
                request.status(),
                request.hubId(),
                request.companyId(),
                pageable,
                direction
        );

        List<DeliveryResponse.Summary> content = page.getContent().stream()
                .map(DeliveryResponse.Summary::from)
                .toList();

        return new DeliveryResponse.PageResult(
                content,
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    private Sort.Direction parseDirection(String sortDirection) {
        if (sortDirection == null) {
            return Sort.Direction.ASC;
        }
        return "DESC".equalsIgnoreCase(sortDirection.trim())
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
    }

    // ================================
    // 허브 경로 FeignClient 응답 DTO 예시
    // ================================
//    public record HubLegResponse(
//            UUID sourceHubId,
//            UUID targetHubId,
//            double estimatedKm,
//            int estimatedMinutes
//    ) { }

    // 실제 FeignClient 인터페이스는 별도 파일로 분리해서 작성하는 걸 추천
//    public interface HubRouteFeignClient {
//        List<HubLegResponse> getRouteLegs(UUID sourceHubId, UUID targetHubId);
//    }
}

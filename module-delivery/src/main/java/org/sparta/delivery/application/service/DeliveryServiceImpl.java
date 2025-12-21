package org.sparta.delivery.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.error.BusinessException;
import org.sparta.common.event.EventPublisher;
import org.sparta.delivery.domain.entity.Delivery;
import org.sparta.delivery.domain.entity.DeliveryCancelRequest;
import org.sparta.delivery.domain.entity.DeliveryOutboxEvent;
import org.sparta.delivery.domain.enumeration.CancelRequestStatus;
import org.sparta.delivery.domain.enumeration.DeliveryStatus;
import org.sparta.delivery.domain.error.DeliveryCancelledException;
import org.sparta.delivery.domain.error.DeliveryErrorType;
import org.sparta.common.event.delivery.DeliveryCreatedEvent;
import org.sparta.delivery.domain.event.publisher.DeliveryCreatedLocalEvent;
import org.sparta.delivery.domain.repository.DeliveryCancelRequestRepository;
import org.sparta.delivery.domain.repository.DeliveryOutBoxEventRepository;
import org.sparta.delivery.domain.repository.DeliveryRepository;
import org.sparta.delivery.domain.repository.DeliveryProcessedEventRepository;
import org.sparta.delivery.infrastructure.client.HubRouteFeignClient;
import org.sparta.delivery.infrastructure.event.OrderApprovedEvent;
import org.sparta.delivery.infrastructure.event.publisher.DeliveryCompletedEvent;
import org.sparta.delivery.infrastructure.event.publisher.DeliveryFailedEvent;
import org.sparta.delivery.infrastructure.event.publisher.DeliveryLastHubArrivedEvent;
import org.sparta.delivery.infrastructure.event.publisher.DeliveryStartedEvent;
import org.sparta.delivery.presentation.dto.request.DeliveryRequest;
import org.sparta.delivery.presentation.dto.response.DeliveryResponse;
import org.sparta.delivery.presentation.dto.response.HubLegResponse;
import org.sparta.deliverylog.application.service.DeliveryLogService;
import org.sparta.deliverylog.presentation.dto.request.DeliveryLogRequest;
import org.sparta.deliverylog.presentation.dto.response.DeliveryLogResponse;
import org.sparta.deliveryman.application.service.DeliveryManService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryServiceImpl implements DeliveryService {

    // Repository
    private final DeliveryRepository deliveryRepository;
    private final DeliveryProcessedEventRepository deliveryProcessedEventRepository;
    private final DeliveryOutBoxEventRepository deliveryOutBoxEventRepository;
    private final DeliveryCancelRequestRepository cancelRequestRepository;  // 배송 삭제 보완을 위해 추가
    // Service
    private final DeliveryLogService deliveryLogService;
    private final DeliveryManService deliveryManService;
    // api
    private final HubRouteFeignClient hubRouteFeignClient;
    // event
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    // ================================
    // 1. 배송 생성 - 단순(API/테스트용)
    // ================================
    @Override
    @Transactional
    public DeliveryResponse.Detail createSimple(DeliveryRequest.Create request) {
        // 주문당 하나만 허용
        if (deliveryRepository.existsByOrderIdAndDeletedAtIsNull(request.orderId())) {
            throw new BusinessException(DeliveryErrorType.DELIVERY_ALREADY_EXISTS);
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

        // 트랜잭션 커밋 후 이벤트 발행
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publishExternal(DeliveryCreatedLocalEvent.of(
                        saved.getOrderId(),
                        saved.getId(),
                        saved.getSupplierHubId(),
                        saved.getReceiveHubId(),
                        saved.getTotalLogSeq()
                ));
                log.info("DeliveryCreatedLocalEvent published after commit: deliveryId={}", saved.getId());
            }
        });

        return DeliveryResponse.Detail.from(saved);
    }

    // ================================
    // 2. 배송 생성 - 허브 경로 + 로그 생성 (멱등성 보장) / (SYSTEM)
    // ================================
    /**
     * OrderApprovedEvent를 받아 배송 생성
     * 멱등성 보장:
     * - Cancel Intent 확인 (PESSIMISTIC_WRITE 락) - 생성과 삭제 이벤트 동시성 문제 처리
     * - eventId 기반 중복 이벤트 체크 (1차 방어)
     * - orderId 기반 중복 배송 체크 (2차 방어)
     * 실패 이벤트 발행 정책:
     * - 복구 불가능한 비즈니스 오류 (NO_ROUTE_AVAILABLE): DeliveryFailedEvent 발행
     * - 일시적 오류 (Feign 타임아웃, DB 연결 오류 등): 재시도 (이벤트 발행 안함)
     * - 중복 이벤트 (DELIVERY_ALREADY_EXISTS): 조용히 무시 (이벤트 발행 안함)
     */
    @Override
    @Transactional
    public DeliveryResponse.Detail createWithRoute(OrderApprovedEvent orderEvent) {
        log.info("Processing OrderApprovedEvent: orderId={}, eventId={}",
                orderEvent.orderId(), orderEvent.eventId());

        try {

            // 0. Cancel Intent 확인 (PESSIMISTIC_WRITE 락)
            Optional<DeliveryCancelRequest> cancelIntent =
                    cancelRequestRepository.findWithLockByOrderId(orderEvent.orderId());

            if (cancelIntent.isPresent() && cancelIntent.get().getStatus() == CancelRequestStatus.REQUESTED) {
                log.warn("Cancel intent detected! Skip delivery creation: orderId={}, cancelEventId={}",
                        orderEvent.orderId(), cancelIntent.get().getCancelEventId());

                // Cancel Intent APPLIED 처리
                cancelIntent.get().markApplied();

                // 배송 생성 중단 예외 발생
                throw new DeliveryCancelledException(orderEvent.orderId());
            }

            // 1차 방어: eventId 기반 멱등성 체크
            if (deliveryProcessedEventRepository.existsByEventId(orderEvent.eventId())) {
                log.info("Event already processed, skipping: eventId={}, orderId={}",
                        orderEvent.eventId(), orderEvent.orderId());

                // 기존 배송 조회 후 반환 (멱등 성공)
                Delivery existing = deliveryRepository
                        .findByOrderIdAndDeletedAtIsNull(orderEvent.orderId())
                        .orElseThrow(() -> new BusinessException(DeliveryErrorType.DELIVERY_NOT_FOUND));

                return DeliveryResponse.Detail.from(existing);
            }

            // 2차 방어: orderId 기반 중복 배송 체크
            Optional<Delivery> existingDelivery =
                    deliveryRepository.findByOrderIdAndDeletedAtIsNull(orderEvent.orderId());

            if (existingDelivery.isPresent()) {
                log.info("Delivery already exists for orderId={}, saving DeliveryProcessedEvent and returning existing delivery",
                        orderEvent.orderId());

//                // 이벤트 기록만 저장하고 기존 배송 반환 (멱등 성공) 멱등은 Listener class에서 저장으로 수정함
//                deliveryProcessedEventRepository.save(
//                        DeliveryProcessedEvent.of(orderEvent.eventId(), "ORDER_APPROVED")
//                );

                return DeliveryResponse.Detail.from(existingDelivery.get());
            }

            // 1) 허브 경로 조회
            List<HubLegResponse> legs = hubRouteFeignClient.getRouteLegs(
                    orderEvent.supplierHubId(),
                    orderEvent.receiveHubId()
            );

            // 복구 불가능한 오류: 경로 없음 → 실패 이벤트 발행
            if (legs == null || legs.isEmpty()) {
                log.error("No route available: supplierHubId={}, receiveHubId={}, orderId={}",
                        orderEvent.supplierHubId(), orderEvent.receiveHubId(), orderEvent.orderId());

                // 실패 이벤트 발행 (주문 쪽에서 처리)
                eventPublisher.publishExternal(DeliveryFailedEvent.of(
                        orderEvent.orderId(),
                        "NO_ROUTE_AVAILABLE"
                ));

                throw new BusinessException(DeliveryErrorType.NO_ROUTE_AVAILABLE);
            }

            // 2) Delivery 생성
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
                    null  // totalLogSeq는 아래에서 설정
            );

            Delivery savedDelivery = deliveryRepository.save(delivery);
            log.info("Delivery created: deliveryId={}, orderId={}",
                    savedDelivery.getId(), savedDelivery.getOrderId());

            // 3) DeliveryLog 생성
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

            // 4) totalLogSeq 업데이트
            savedDelivery.updateTotalLogSeq(sequence);
            log.info("DeliveryLogs created: deliveryId={}, totalLogSeq={}",
                    savedDelivery.getId(), sequence);

//            // 5) 이벤트 처리 완료 기록 (같은 트랜잭션)
//            deliveryProcessedEventRepository.save(
//                    DeliveryProcessedEvent.of(orderEvent.eventId(), "ORDER_APPROVED")
//            );

            // ===================== DeliveryCreatedEvent 외부 이벤트 발행 준비 =====================

            // 5) Outbox 이벤트 저장 (Delivery 생성 후!)
            log.info("[이벤트] 발행 준비");
            log.info("[이벤트] - orderId: {}", delivery.getOrderId());

            DeliveryCreatedEvent event = DeliveryCreatedEvent.of(
                    delivery.getId(),
                    delivery.getOrderId()
            );

            // ===================== Outbox 패턴 적용 =====================
            String payload;
            try {
                payload = objectMapper.writeValueAsString(event);
            } catch (JsonProcessingException e) {
                log.error("[Outbox] 이벤트 직렬화 실패", e);
                throw new RuntimeException("DeliveryCreatedEvent 직렬화 실패", e);
            }

            DeliveryOutboxEvent outbox = DeliveryOutboxEvent.ready(
                    "DELIVERY",
                    delivery.getId(),
                    "DeliveryCreatedEvent",
                    payload
            );

            deliveryOutBoxEventRepository.save(outbox);

            log.info("[Outbox] 이벤트 저장 완료 - outboxId={}, deliveryId={}, status=READY",
                    outbox.getId(), delivery.getId());

            //            // 6) 이벤트 처리 완료 기록 (마지막에!)
//            deliveryProcessedEventRepository.save(
//                    DeliveryProcessedEvent.of(orderEvent.eventId(), "DELIVERY_CREATED")
//            );

            // 7) 트랜잭션 커밋 후 로컬 이벤트 발행
            UUID deliveryId = savedDelivery.getId();
            UUID orderId = savedDelivery.getOrderId();
            UUID supplierHubId = savedDelivery.getSupplierHubId();
            UUID receiveHubId = savedDelivery.getReceiveHubId();
            Integer totalLogSeq = savedDelivery.getTotalLogSeq();

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eventPublisher.publishLocal(DeliveryCreatedLocalEvent.of(
                            orderId,
                            deliveryId,
                            supplierHubId,
                            receiveHubId,
                            totalLogSeq
                    ));
                    log.info("DeliveryCreatedLocalEvent published after transaction commit: deliveryId={}, orderId={}",
                            deliveryId, orderId);

                }
            });

            log.info("Delivery creation completed successfully: deliveryId={}, orderId={}, eventId={}",
                    savedDelivery.getId(), savedDelivery.getOrderId(), orderEvent.eventId());

            return DeliveryResponse.Detail.from(savedDelivery);

        } catch (BusinessException e) {
            // 비즈니스 예외: 로그만 남기고 재던지기
            log.warn("Business validation failed: orderId={}, errorType={}, message={}",
                    orderEvent.orderId(), e.getErrorType(), e.getMessage());
            throw e;

        } catch (feign.RetryableException e) {
            // 일시적 네트워크 오류: 재시도 가능 (실패 이벤트 발행 안함)
            log.error("Retryable Feign error creating delivery: orderId={}, message={}",
                    orderEvent.orderId(), e.getMessage(), e);
            throw new RuntimeException("Temporary network error - will retry", e);

        } catch (Exception e) {
            // 예기치 못한 오류: 실패 이벤트 발행 + 재던지기
            log.error("Unexpected error creating delivery: orderId={}, eventId={}",
                    orderEvent.orderId(), orderEvent.eventId(), e);

            // 실패 이벤트 발행
            eventPublisher.publishExternal(DeliveryFailedEvent.of(
                    orderEvent.orderId(),
                    "UNEXPECTED_ERROR"
            ));

            throw new BusinessException(DeliveryErrorType.CREATION_FAILED);
        }
    }

    // ================================
    // 3. 허브 배송 담당자 배정
    // ================================
    @Override
    @Transactional
    public DeliveryResponse.Detail assignHubDeliveryMan(UUID deliveryId, DeliveryRequest.AssignHubDeliveryMan request) {

        log.info(
                "[TX-CHECK] name={}, active={}, readOnly={}",
                TransactionSynchronizationManager.getCurrentTransactionName(),
                TransactionSynchronizationManager.isActualTransactionActive(),
                TransactionSynchronizationManager.isCurrentTransactionReadOnly()
        );

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

        // DeliveryLog 상태 전이: HUB_WAITING -> HUB_MOVING
        List<DeliveryLogResponse.Summary> timeline = deliveryLogService.getTimelineByDeliveryId(deliveryId);
        DeliveryLogResponse.Summary target = timeline.stream()
                .filter(log -> log.sequence() == seq)
                .findFirst()
                .orElseThrow(() -> new BusinessException(DeliveryErrorType.INVALID_LOG_SEQUENCE));

        deliveryLogService.startLog(target.id());

        // [ 배송 시작 이벤트 ] 첫 허브 출발 시에만 발행
        if (request.sequence() == 0) {
            eventPublisher.publishExternal(DeliveryStartedEvent.of(
                    delivery.getOrderId(),
                    delivery.getId(),
                    delivery.getSupplierHubId()
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

        // [ 마지막 허브 도착 시 업체 배송 담당자 배정을 위한 이벤트 발행 ]
        if (isLastLog) {
            eventPublisher.publishExternal(DeliveryLastHubArrivedEvent.of(
                    delivery.getOrderId(),
                    delivery.getId(),
                    delivery.getReceiveHubId()
            ));
        }

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
        eventPublisher.publishExternal(DeliveryCompletedEvent.of(
                delivery.getOrderId(),
                delivery.getId(),
                delivery.getReceiveCompanyId()
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

    /**
     * 추가된 메서드: 배송 취소 시도 (예외 발생 안 함)
     */
    @Override
    @Transactional
    public boolean cancelIfExists(UUID orderId) {
        log.info("Attempting to cancel delivery if exists: orderId={}", orderId);

        // 1. Delivery 조회
        Optional<Delivery> deliveryOpt = deliveryRepository.findByOrderIdAndDeletedAtIsNull(orderId);

        if (deliveryOpt.isEmpty()) {
            log.info("Delivery not found for orderId={}", orderId);
            return false; // 배송 없음
        }

        Delivery delivery = deliveryOpt.get();

        // 2. 이미 취소됨 체크 (멱등성)
        if (delivery.getStatus() == DeliveryStatus.CANCELED) {
            log.info("Delivery already cancelled: deliveryId={}", delivery.getId());
            return true; // 이미 취소됨
        }

        // 3. 취소 가능 여부 확인
        if (!canBeCancelled(delivery)) {
            log.warn("Delivery cannot be cancelled: deliveryId={}, status={}",
                    delivery.getId(), delivery.getStatus());
            return false; // 취소 불가 (이미 배송 시작등 취소 가능한 status가 아닌 경우)
        }

        // 4. 취소 처리 (기존 헬퍼 메서드 재사용)
        log.info("Cancelling delivery: deliveryId={}, orderId={}", delivery.getId(), orderId);

        delivery.cancel();
        cancelDeliveryLogs(delivery.getId());
        unassignDeliveryMan(delivery);

        log.info("Delivery cancelled successfully: deliveryId={}", delivery.getId());
        return true;
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

    // =======================
    // # 헬퍼 메서드
    // ==========================

    private Sort.Direction parseDirection(String sortDirection) {
        if (sortDirection == null) {
            return Sort.Direction.ASC;
        }
        return "DESC".equalsIgnoreCase(sortDirection.trim())
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
    }

    /**
     * 취소 가능 여부 확인
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
            // 로그 취소 실패해도 진행 (실패 -> 스케줄러가 처리 예정)
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
            // 담당자 해제 실패해도 진행 (실패 -> 스케줄러가 처리 예정)
        }
    }
}
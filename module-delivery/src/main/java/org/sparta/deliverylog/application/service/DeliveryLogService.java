package org.sparta.deliverylog.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.error.BusinessException;
import org.sparta.deliverylog.application.dto.DeliveryLogRequest;
import org.sparta.deliverylog.application.dto.DeliveryLogResponse;
import org.sparta.deliverylog.application.event.DeliveryLogEventPublisher;
import org.sparta.deliverylog.domain.entity.DeliveryLog;
import org.sparta.deliverylog.domain.enumeration.DeliveryRouteStatus;
import org.sparta.deliverylog.exception.DeliveryLogErrorType;
import org.sparta.deliverylog.infrastructure.repository.DeliveryLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryLogService {

    private final DeliveryLogRepository deliveryLogRepository;
    private final DeliveryLogEventPublisher eventPublisher;

    /**
     * 배송 경로 생성
     */
    @Transactional
    public DeliveryLogResponse.Detail createDeliveryLog(DeliveryLogRequest.Create request) {
        DeliveryLog deliveryLog = DeliveryLog.create(
                request.deliveryId(),
                request.hubSequence(),
                request.departureHubId(),
                request.destinationHubId(),
                request.expectedDistance(),
                request.expectedTime()
        );

        DeliveryLog saved = deliveryLogRepository.save(deliveryLog);

        // Kafka 이벤트 발행
        eventPublisher.publishDeliveryLogCreated(saved);

        log.info("배송 경로 생성 완료: {}", saved.getDeliveryLogId());
        return new DeliveryLogResponse.Detail(saved);
    }

    /**
     * 배송 담당자 배정
     */
    @Transactional
    public DeliveryLogResponse.Detail assignDeliveryMan(UUID deliveryLogId, UUID deliveryManId) {
        DeliveryLog deliveryLog = findDeliveryLogById(deliveryLogId);
        deliveryLog.assignDeliveryMan(deliveryManId);

        // Kafka 이벤트 발행
        eventPublisher.publishDeliveryManAssigned(deliveryLog);

        log.info("배송 담당자 배정 완료: {} -> {}", deliveryLogId, deliveryManId);
        return new DeliveryLogResponse.Detail(deliveryLog);
    }

    /**
     * 배송 시작
     */
    @Transactional
    public DeliveryLogResponse.Detail startDelivery(UUID deliveryLogId) {
        DeliveryLog deliveryLog = findDeliveryLogById(deliveryLogId);
        deliveryLog.startDelivery();

        // Kafka 이벤트 발행
        eventPublisher.publishDeliveryStarted(deliveryLog);

        log.info("배송 시작: {}", deliveryLogId);
        return new DeliveryLogResponse.Detail(deliveryLog);
    }

    /**
     * 배송 완료
     */
    @Transactional
    public DeliveryLogResponse.Detail completeDelivery(
            UUID deliveryLogId,
            DeliveryLogRequest.Complete request
    ) {
        DeliveryLog deliveryLog = findDeliveryLogById(deliveryLogId);
        deliveryLog.completeDelivery(request.actualDistance(), request.actualTime());

        // Kafka 이벤트 발행
        eventPublisher.publishDeliveryCompleted(deliveryLog);

        log.info("배송 완료: {}", deliveryLogId);
        return new DeliveryLogResponse.Detail(deliveryLog);
    }

    /**
     * 배송 경로 단건 조회
     */
    public DeliveryLogResponse.Detail getDeliveryLog(UUID deliveryLogId) {
        DeliveryLog deliveryLog = findDeliveryLogById(deliveryLogId);
        return new DeliveryLogResponse.Detail(deliveryLog);
    }

    /**
     * 배송 ID로 전체 경로 조회
     */
    public List<DeliveryLogResponse.Summary> getDeliveryLogsByDeliveryId(UUID deliveryId) {
        List<DeliveryLog> deliveryLogs = deliveryLogRepository.findByDeliveryIdOrderByHubSequence(deliveryId);
        return deliveryLogs.stream()
                .map(DeliveryLogResponse.Summary::new)
                .collect(Collectors.toList());
    }

    /**
     * 배송 담당자의 진행 중인 경로 조회
     */
    public List<DeliveryLogResponse.Summary> getDeliveryManInProgressLogs(UUID deliveryManId) {
        List<DeliveryRouteStatus> inProgressStatuses = Arrays.asList(
                DeliveryRouteStatus.WAITING,
                DeliveryRouteStatus.MOVING
        );
        List<DeliveryLog> deliveryLogs = deliveryLogRepository
                .findByDeliveryManIdAndDeliveryStatusIn(deliveryManId, inProgressStatuses);

        return deliveryLogs.stream()
                .map(DeliveryLogResponse.Summary::new)
                .collect(Collectors.toList());
    }

    /**
     * 허브의 대기 중인 경로 조회
     */
    public List<DeliveryLogResponse.Summary> getHubWaitingLogs(UUID hubId) {
        List<DeliveryLog> deliveryLogs = deliveryLogRepository
                .findByDepartureHubIdAndDeliveryStatus(hubId, DeliveryRouteStatus.WAITING);

        return deliveryLogs.stream()
                .map(DeliveryLogResponse.Summary::new)
                .collect(Collectors.toList());
    }

    /**
     * 전체 경로 목록 조회 (페이징)
     */
    public Page<DeliveryLogResponse.Summary> getAllDeliveryLogs(Pageable pageable) {
        Page<DeliveryLog> deliveryLogs = deliveryLogRepository.findAllActive(pageable);
        return deliveryLogs.map(DeliveryLogResponse.Summary::new);
    }

    /**
     * 배송 경로 취소
     */
    @Transactional
    public void cancelDeliveryLog(UUID deliveryLogId) {
        DeliveryLog deliveryLog = findDeliveryLogById(deliveryLogId);
        deliveryLog.cancel();

        // Kafka 이벤트 발행
        eventPublisher.publishDeliveryCanceled(deliveryLog);

        log.info("배송 경로 취소: {}", deliveryLogId);
    }

    /**
     * 배송 경로 삭제 (논리 삭제)
     */
    @Transactional
    public void deleteDeliveryLog(UUID deliveryLogId) {
        DeliveryLog deliveryLog = findDeliveryLogById(deliveryLogId);
        deliveryLogRepository.delete(deliveryLog);
        log.info("배송 경로 삭제: {}", deliveryLogId);
    }

    // ========== Private Methods ==========

    private DeliveryLog findDeliveryLogById(UUID deliveryLogId) {
        return deliveryLogRepository.findById(deliveryLogId)
                .orElseThrow(() -> new BusinessException(DeliveryLogErrorType.DELIVERY_LOG_NOT_FOUND));
    }
}

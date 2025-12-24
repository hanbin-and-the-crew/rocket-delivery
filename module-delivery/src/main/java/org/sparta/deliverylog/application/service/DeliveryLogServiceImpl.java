package org.sparta.deliverylog.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.error.BusinessException;
import org.sparta.deliverylog.domain.entity.DeliveryLog;
import org.sparta.deliverylog.domain.error.DeliveryLogErrorType;
import org.sparta.deliverylog.domain.repository.DeliveryLogRepository;
import org.sparta.deliverylog.presentation.dto.request.DeliveryLogRequest;
import org.sparta.deliverylog.presentation.dto.response.DeliveryLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryLogServiceImpl implements DeliveryLogService {

    private final DeliveryLogRepository deliveryLogRepository;

    // ================================
    // 1. 생성 (관리자/테스트용)
    // ================================
    @Override
    @Transactional
    public DeliveryLogResponse.Detail create(DeliveryLogRequest.Create request) {

        // 동일 deliveryId 내 sequence 중복 방지
        if (deliveryLogRepository.existsByDeliveryIdAndSequenceAndDeletedAtIsNull(
                request.deliveryId(), request.sequence())) {
            throw new BusinessException(DeliveryLogErrorType.SEQUENCE_REQUIRED);
        }

        DeliveryLog log = DeliveryLog.create(
                request.deliveryId(),
                request.sequence(),
                request.sourceHubId(),
                request.targetHubId(),
                request.estimatedKm(),
                request.estimatedMinutes()
        );

        DeliveryLog saved = deliveryLogRepository.save(log);
        return DeliveryLogResponse.Detail.from(saved);
    }

    // ================================
    // 2. 허브 담당자 배정 (CREATED -> HUB_WAITING)
    // ================================
    @Override
    @Transactional
    public DeliveryLogResponse.Detail assignDeliveryMan(UUID logId, DeliveryLogRequest.AssignDeliveryMan request) {

        log.info(
                "[TX-CHECK] name={}, active={}, readOnly={}",
                TransactionSynchronizationManager.getCurrentTransactionName(),
                TransactionSynchronizationManager.isActualTransactionActive(),
                TransactionSynchronizationManager.isCurrentTransactionReadOnly()
        );

        DeliveryLog log = deliveryLogRepository.findByIdAndDeletedAtIsNull(logId)
                .orElseThrow(() -> new BusinessException(DeliveryLogErrorType.DELIVERY_ID_REQUIRED));

        log.assignDeliveryMan(request.deliveryManId());
        return DeliveryLogResponse.Detail.from(log);
    }

    // ================================
    // 3. 허브 leg 출발 (HUB_WAITING -> HUB_MOVING)
    // ================================
    @Override
    @Transactional
    public DeliveryLogResponse.Detail startLog(UUID logId) {
        DeliveryLog log = deliveryLogRepository.findByIdAndDeletedAtIsNull(logId)
                .orElseThrow(() -> new BusinessException(DeliveryLogErrorType.DELIVERY_ID_REQUIRED));

        log.markMoving();
        return DeliveryLogResponse.Detail.from(log);
    }

    // ================================
    // 4. 허브 leg 도착 (HUB_MOVING -> HUB_ARRIVED)
    // ================================
    @Override
    @Transactional
    public DeliveryLogResponse.Detail arriveLog(UUID logId, DeliveryLogRequest.Arrive request) {
        DeliveryLog log = deliveryLogRepository.findByIdAndDeletedAtIsNull(logId)
                .orElseThrow(() -> new BusinessException(DeliveryLogErrorType.DELIVERY_ID_REQUIRED));

        log.markArrived(request.actualKm(), request.actualMinutes());
        return DeliveryLogResponse.Detail.from(log);
    }

    // ================================
    // 5. Delivery 취소에 따른 로그 취소
    // ================================
    @Override
    @Transactional
    public DeliveryLogResponse.Detail cancelFromDelivery(UUID logId) {
        DeliveryLog log = deliveryLogRepository.findByIdAndDeletedAtIsNull(logId)
                .orElseThrow(() -> new BusinessException(DeliveryLogErrorType.DELIVERY_ID_REQUIRED));

        log.cancelFromDelivery();
        return DeliveryLogResponse.Detail.from(log);
    }


    /**
     * 배송 취소 시 해당 배송의 모든 로그 취소
     * - CREATED, HUB_WAITING 상태만 취소 가능
     * - HUB_MOVING 이후 상태는 취소 불가 (경고 로그만)
     */
    @Override
    @Transactional
    public void cancelAllLogsByDeliveryId(UUID deliveryId) {
        log.info("Cancelling all DeliveryLogs for deliveryId={}", deliveryId);

        List<DeliveryLog> logs = deliveryLogRepository.findByDeliveryIdAndDeletedAtIsNull(deliveryId);

        if (logs.isEmpty()) {
            log.warn("No DeliveryLogs found for deliveryId={}", deliveryId);
            return;
        }

        int cancelledCount = 0;
        int skippedCount = 0;

        for (DeliveryLog aLog : logs) {
            try {
                aLog.cancelFromDelivery(); // CREATED, HUB_WAITING만 취소 가능
                cancelledCount++;
                log.debug("DeliveryLog cancelled: logId={}, sequence={}, status={}",
                        aLog.getId(), aLog.getSequence(), aLog.getStatus());
            } catch (BusinessException e) {
                // HUB_MOVING, HUB_ARRIVED, CANCELED 상태는 취소 불가
                log.warn("Cannot cancel DeliveryLog: logId={}, sequence={}, status={}, reason={}",
                        aLog.getId(), aLog.getSequence(), aLog.getStatus(), e.getMessage());
                skippedCount++;
                // 계속 진행 (다른 로그는 취소 시도)
            }
        }

        log.info("DeliveryLog cancellation completed: deliveryId={}, total={}, cancelled={}, skipped={}",
                deliveryId, logs.size(), cancelledCount, skippedCount);
    }

    // ================================
    // 6. 단건 조회
    // ================================
    @Override
    public DeliveryLogResponse.Detail getDetail(UUID logId) {
        DeliveryLog log = deliveryLogRepository.findByIdAndDeletedAtIsNull(logId)
                .orElseThrow(() -> new BusinessException(DeliveryLogErrorType.DELIVERY_ID_REQUIRED));

        return DeliveryLogResponse.Detail.from(log);
    }

    // ================================
    // 7. Delivery별 타임라인 조회
    // ================================
    @Override
    public List<DeliveryLogResponse.Summary> getTimelineByDeliveryId(UUID deliveryId) {
        List<DeliveryLog> logs = deliveryLogRepository
                .findAllByDeliveryIdOrderBySequenceAsc(deliveryId);

        return logs.stream()
                .map(DeliveryLogResponse.Summary::from)
                .toList();
    }

    // ================================
    // 8. 검색 + 페이징
    // ================================
    @Override
    public DeliveryLogResponse.PageResult search(DeliveryLogRequest.Search request, Pageable pageable) {
        Sort.Direction direction = parseDirection(request.sortDirection());

        Page<DeliveryLog> page = deliveryLogRepository.search(
                request.hubId(),
                request.deliveryManId(),
                request.deliveryId(),
                pageable,
                direction
        );

        List<DeliveryLogResponse.Summary> content = page.getContent().stream()
                .map(DeliveryLogResponse.Summary::from)
                .toList();

        return new DeliveryLogResponse.PageResult(
                content,
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    // ================================
    // 9. 삭제 (Soft delete)
    // ================================
    @Override
    @Transactional
    public void delete(UUID logId) {
        DeliveryLog log = deliveryLogRepository.findByIdAndDeletedAtIsNull(logId)
                .orElseThrow(() -> new BusinessException(DeliveryLogErrorType.DELIVERY_ID_REQUIRED));

        log.delete(); // 엔티티의 soft delete 메서드
    }

    private Sort.Direction parseDirection(String sortDirection) {
        if (sortDirection == null) {
            return Sort.Direction.ASC;
        }
        return "DESC".equalsIgnoreCase(sortDirection.trim())
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
    }
}

package org.sparta.delivery.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.delivery.domain.entity.DeliveryCancelRequest;
import org.sparta.delivery.domain.entity.DeliveryProcessedEvent;
import org.sparta.delivery.domain.repository.DeliveryCancelRequestRepository;
import org.sparta.delivery.domain.repository.DeliveryProcessedEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryCancelRequestTxService {

    private final DeliveryCancelRequestRepository cancelRequestRepository;
    private final DeliveryProcessedEventRepository deliveryProcessedEventRepository;
//
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    public void saveCancelRequestIfNotExists(UUID orderId, UUID eventId) {
//        if (!cancelRequestRepository.existsByCancelEventIdAndDeletedAtIsNull(eventId)) {
//            DeliveryCancelRequest cancelRequest =
//                    DeliveryCancelRequest.requested(orderId, eventId);
//            cancelRequestRepository.save(cancelRequest);
//        }
//    }
//
//    @Transactional
//    public void markCancelRequestAsApplied(UUID orderId) {
//        cancelRequestRepository.findByOrderIdAndDeletedAtIsNull(orderId)
//                .ifPresent(request -> {
//                    request.markApplied();
//                    log.info("Cancel Request marked as APPLIED: orderId={}", orderId);
//                });
//    }
//
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    public void saveProcessedEvent(String eventId, String eventType) {
//        deliveryProcessedEventRepository.save(
//                DeliveryProcessedEvent.of(eventId, eventType)
//        );
//    }

    /**
     * Cancel Request 저장 (별도 트랜잭션)
     * - 이미 존재하면 저장하지 않음
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveCancelRequestIfNotExists(UUID orderId, UUID eventId) {
        if (cancelRequestRepository.existsByOrderIdAndDeletedAtIsNull(orderId)) {
            log.info("Cancel Request already exists: orderId={}", orderId);
            return;
        }

        DeliveryCancelRequest request = DeliveryCancelRequest.requested(orderId, eventId);
        cancelRequestRepository.save(request);
        log.info("Cancel Request saved: orderId={}, eventId={}", orderId, eventId);
    }

    /**
     * Cancel Request를 APPLIED로 마킹 (별도 트랜잭션)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCancelRequestAsApplied(UUID orderId) {
        cancelRequestRepository.findByOrderIdAndDeletedAtIsNull(orderId)
                .ifPresent(request -> {
                    request.markApplied();
                    log.info("Cancel Request marked as APPLIED: orderId={}", orderId);
                });
    }

    /**
     * 처리된 이벤트 저장 (별도 트랜잭션)
     * - 멱등성 보장을 위한 기록
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveProcessedEvent(UUID eventId, String eventType) {
        deliveryProcessedEventRepository.save(
                DeliveryProcessedEvent.of(eventId, eventType)
        );
        log.info("ProcessedEvent saved: eventId={}, eventType={}", eventId, eventType);
    }

}

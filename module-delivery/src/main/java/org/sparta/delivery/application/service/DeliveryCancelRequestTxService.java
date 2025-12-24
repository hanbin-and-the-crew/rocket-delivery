package org.sparta.delivery.application.service;

import lombok.RequiredArgsConstructor;
import org.sparta.delivery.domain.entity.DeliveryCancelRequest;
import org.sparta.delivery.domain.repository.DeliveryCancelRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryCancelRequestTxService {

    private final DeliveryCancelRequestRepository cancelRequestRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveCancelRequestIfNotExists(UUID orderId, UUID eventId) {
        if (!cancelRequestRepository.existsByCancelEventIdAndDeletedAtIsNull(eventId)) {
            DeliveryCancelRequest cancelRequest =
                    DeliveryCancelRequest.requested(orderId, eventId);
            cancelRequestRepository.save(cancelRequest);
        }
    }
}

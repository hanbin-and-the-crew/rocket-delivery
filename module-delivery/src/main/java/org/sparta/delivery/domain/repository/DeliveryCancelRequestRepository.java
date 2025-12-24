package org.sparta.delivery.domain.repository;

import org.sparta.delivery.domain.entity.DeliveryCancelRequest;
import org.sparta.delivery.domain.enumeration.CancelRequestStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryCancelRequestRepository {

    DeliveryCancelRequest save(DeliveryCancelRequest cancelRequest);

    boolean existsByCancelEventIdAndDeletedAtIsNull(UUID cancelEventId);

    Optional<DeliveryCancelRequest> findByOrderIdAndDeletedAtIsNull(UUID orderId);

    Optional<DeliveryCancelRequest> findWithLockByOrderId(UUID orderId);

    List<DeliveryCancelRequest> findAllByStatusAndDeletedAtIsNull(CancelRequestStatus status);

    DeliveryCancelRequest saveAndFlush(DeliveryCancelRequest entity);

    long countPendingPaymentCancelDlt(CancelRequestStatus status, LocalDateTime cutoffTime);

    // 수정: boolean 반환 타입에서 boolean 반환으로 변경
    boolean existsByOrderIdAndDeletedAtIsNull(UUID orderId);
}

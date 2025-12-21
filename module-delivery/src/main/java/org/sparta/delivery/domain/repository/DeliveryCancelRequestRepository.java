package org.sparta.delivery.domain.repository;

import org.sparta.delivery.domain.entity.DeliveryCancelRequest;
import org.sparta.delivery.domain.enumeration.CancelRequestStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryCancelRequestRepository {

    boolean existsByCancelEventIdAndDeletedAtIsNull(UUID cancelEventId);

    Optional<DeliveryCancelRequest> findByOrderIdAndDeletedAtIsNull(UUID orderId);

    Optional<DeliveryCancelRequest> findWithLockByOrderId(UUID orderId);

    List<DeliveryCancelRequest> findAllByStatusAndDeletedAtIsNull(CancelRequestStatus status);
}

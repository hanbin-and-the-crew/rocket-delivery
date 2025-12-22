package org.sparta.delivery.domain.repository;

import org.sparta.delivery.domain.entity.DeliveryCancelRequest;
import org.sparta.delivery.domain.enumeration.CancelRequestStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryCancelRequestRepository {

    DeliveryCancelRequest save(DeliveryCancelRequest cancelRequest);

    boolean existsByCancelEventIdAndDeletedAtIsNull(UUID cancelEventId);

    Optional<DeliveryCancelRequest> findByOrderIdAndDeletedAtIsNull(UUID orderId);

    Optional<DeliveryCancelRequest> findWithLockByOrderId(UUID orderId);

    List<DeliveryCancelRequest> findAllByStatusAndDeletedAtIsNull(CancelRequestStatus status);

//    DeliveryCancelRequest saveAndFlush(DeliveryCancelRequest entity);
//    <S extends DeliveryCancelRequest> List<S> saveAndFlush(Iterable<S> entities);
//    void flush();

    DeliveryCancelRequest saveAndFlush(DeliveryCancelRequest entity);
}

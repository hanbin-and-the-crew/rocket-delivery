package org.sparta.deliverylog.domain.repository;

import org.sparta.deliverylog.domain.entity.DeliveryLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryLogRepository {

    DeliveryLog save(DeliveryLog deliveryLog);

    Optional<DeliveryLog> findByIdAndDeletedAtIsNull(UUID id);

    List<DeliveryLog> findAllByDeliveryIdOrderBySequenceAsc(UUID deliveryId);

    boolean existsByDeliveryIdAndSequenceAndDeletedAtIsNull(UUID deliveryId, int sequence);

    Page<DeliveryLog> search(
            UUID hubId,
            UUID deliveryManId,
            UUID deliveryId,
            Pageable pageable,
            Sort.Direction direction
    );
}

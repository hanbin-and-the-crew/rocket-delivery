package org.sparta.deliverylog.infrastructure.repository;

import org.sparta.deliverylog.domain.entity.DeliveryLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryLogJpaRepository extends JpaRepository<DeliveryLog, UUID> {

    Optional<DeliveryLog> findByIdAndDeletedAtIsNull(UUID id);

    List<DeliveryLog> findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceAsc(UUID deliveryId);

    boolean existsByDeliveryIdAndSequenceAndDeletedAtIsNull(UUID deliveryId, int sequence);

    List<DeliveryLog> findByDeliveryIdAndDeletedAtIsNull(UUID deliveryId);
}

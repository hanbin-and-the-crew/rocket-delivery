package org.sparta.deliveryman.domain.repository;

import org.sparta.deliveryman.domain.entity.DeliveryMan;
import org.sparta.deliveryman.domain.enumeration.DeliveryManStatus;
import org.sparta.deliveryman.domain.enumeration.DeliveryManType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryManRepository {

    Optional<DeliveryMan> findByIdAndDeletedAtIsNull(UUID id);

    Optional<DeliveryMan> findByUserIdAndDeletedAtIsNull(UUID userId);

    Integer findMaxSequenceByTypeAndDeletedAtIsNull(DeliveryManType type);

    Integer findMaxSequenceByHubIdAndTypeAndDeletedAtIsNull(UUID hubId, DeliveryManType type);

    List<DeliveryMan> findAllByTypeAndDeletedAtIsNullOrderBySequenceAsc(DeliveryManType type);

    List<DeliveryMan> findAllByHubIdAndTypeAndDeletedAtIsNullOrderBySequenceAsc(UUID hubId, DeliveryManType type);

    List<DeliveryMan> search(UUID hubId,
                             DeliveryManType type,
                             DeliveryManStatus status,
                             String realName);

    boolean existsByUserIdAndDeletedAtIsNull(UUID userId);

    DeliveryMan save(DeliveryMan deliveryMan);
}

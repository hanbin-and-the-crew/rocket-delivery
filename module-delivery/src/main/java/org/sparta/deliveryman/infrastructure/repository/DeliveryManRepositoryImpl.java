package org.sparta.deliveryman.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.sparta.deliveryman.domain.entity.DeliveryMan;
import org.sparta.deliveryman.domain.enumeration.DeliveryManStatus;
import org.sparta.deliveryman.domain.enumeration.DeliveryManType;
import org.sparta.deliveryman.domain.repository.DeliveryManRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DeliveryManRepositoryImpl implements DeliveryManRepository {

    private final DeliveryManJpaRepository jpaRepository;

    @Override
    public Optional<DeliveryMan> findByIdAndDeletedAtIsNull(UUID id) {
        return jpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public Optional<DeliveryMan> findByUserIdAndDeletedAtIsNull(UUID userId) {
        return jpaRepository.findByUserIdAndDeletedAtIsNull(userId);
    }

    @Override
    public Integer findMaxSequenceByTypeAndDeletedAtIsNull(DeliveryManType type) {
        return jpaRepository.findMaxSequenceByTypeAndDeletedAtIsNull(type);
    }

    @Override
    public Integer findMaxSequenceByHubIdAndTypeAndDeletedAtIsNull(UUID hubId, DeliveryManType type) {
        return jpaRepository.findMaxSequenceByHubIdAndTypeAndDeletedAtIsNull(hubId, type);
    }

    @Override
    public List<DeliveryMan> findAllByTypeAndDeletedAtIsNullOrderBySequenceAsc(DeliveryManType type) {
        return jpaRepository.findAllByTypeAndDeletedAtIsNullOrderBySequenceAsc(type);
    }

    @Override
    public List<DeliveryMan> findAllByHubIdAndTypeAndDeletedAtIsNullOrderBySequenceAsc(UUID hubId, DeliveryManType type) {
        return jpaRepository.findAllByHubIdAndTypeAndDeletedAtIsNullOrderBySequenceAsc(hubId, type);
    }

    @Override
    public List<DeliveryMan> search(UUID hubId,
                                    DeliveryManType type,
                                    DeliveryManStatus status,
                                    String realName) {
        return jpaRepository.search(hubId, type, status, realName);
    }

    @Override
    public boolean existsByUserIdAndDeletedAtIsNull(UUID userId) {
        return jpaRepository.existsByUserIdAndDeletedAtIsNull(userId);
    }

    @Override
    public DeliveryMan save(DeliveryMan deliveryMan) {
        return jpaRepository.save(deliveryMan);
    }
}

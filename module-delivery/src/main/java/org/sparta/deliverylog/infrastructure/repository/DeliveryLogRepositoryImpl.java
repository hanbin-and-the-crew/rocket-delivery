package org.sparta.deliverylog.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.sparta.deliverylog.domain.entity.DeliveryLog;
import org.sparta.deliverylog.domain.enumeration.DeliveryRouteStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DeliveryLogRepositoryImpl implements DeliveryLogRepository {

    private final DeliveryLogJpaRepository jpaRepository;

    @Override
    public DeliveryLog save(DeliveryLog deliveryLog) {
        return jpaRepository.save(deliveryLog);
    }

    @Override
    public Optional<DeliveryLog> findById(UUID deliveryLogId) {
        return jpaRepository.findByIdAndNotDeleted(deliveryLogId);
    }

    @Override
    public List<DeliveryLog> findByDeliveryIdOrderByHubSequence(UUID deliveryId) {
        return jpaRepository.findByDeliveryIdOrderByHubSequence(deliveryId);
    }

    @Override
    public List<DeliveryLog> findByDeliveryManIdAndDeliveryStatusIn(
            UUID deliveryManId,
            List<DeliveryRouteStatus> statuses
    ) {
        return jpaRepository.findByDeliveryManIdAndDeliveryStatusIn(deliveryManId, statuses);
    }

    @Override
    public List<DeliveryLog> findByDepartureHubIdAndDeliveryStatus(
            UUID hubId,
            DeliveryRouteStatus status
    ) {
        return jpaRepository.findByDepartureHubIdAndDeliveryStatus(hubId, status);
    }

    @Override
    public Page<DeliveryLog> findAllActive(Pageable pageable) {
        return jpaRepository.findAllActive(pageable);
    }

    @Override
    public void delete(DeliveryLog deliveryLog) {
        deliveryLog.markAsDeleted();
        jpaRepository.save(deliveryLog);
    }
}

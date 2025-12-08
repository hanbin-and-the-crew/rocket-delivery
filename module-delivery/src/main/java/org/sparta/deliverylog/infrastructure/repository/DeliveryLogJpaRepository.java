package org.sparta.deliverylog.infrastructure.repository;

import org.sparta.deliverylog.domain.entity.DeliveryLog;
import org.sparta.deliverylog.domain.enumeration.DeliveryRouteStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryLogJpaRepository extends JpaRepository<DeliveryLog, UUID> {

    @Query("SELECT d FROM DeliveryLog d WHERE d.deliveryLogId = :id AND d.deletedAt IS NULL")
    Optional<DeliveryLog> findByIdAndNotDeleted(UUID id);

    @Query("SELECT d FROM DeliveryLog d WHERE d.deliveryId = :deliveryId AND d.deletedAt IS NULL ORDER BY d.hubSequence ASC")
    List<DeliveryLog> findByDeliveryIdOrderByHubSequence(UUID deliveryId);

    @Query("SELECT d FROM DeliveryLog d WHERE d.deliveryManId = :deliveryManId AND d.deliveryStatus IN :statuses AND d.deletedAt IS NULL")
    List<DeliveryLog> findByDeliveryManIdAndDeliveryStatusIn(UUID deliveryManId, List<DeliveryRouteStatus> statuses);

    @Query("SELECT d FROM DeliveryLog d WHERE d.departureHubId = :hubId AND d.deliveryStatus = :status AND d.deletedAt IS NULL")
    List<DeliveryLog> findByDepartureHubIdAndDeliveryStatus(UUID hubId, DeliveryRouteStatus status);

    @Query("SELECT d FROM DeliveryLog d WHERE d.deletedAt IS NULL ORDER BY d.createdAt DESC, d.deliveryLogId DESC")
    Page<DeliveryLog> findAllActive(Pageable pageable);
}

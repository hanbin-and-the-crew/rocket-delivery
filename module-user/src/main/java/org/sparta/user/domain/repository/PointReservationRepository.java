package org.sparta.user.domain.repository;

import org.sparta.user.domain.entity.PointReservation;
import org.sparta.user.domain.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PointReservationRepository extends JpaRepository<PointReservation, UUID> {
    List<PointReservation> findByOrderIdAndStatus(UUID orderId, ReservationStatus status);
    boolean existsByOrderId(UUID orderId);
    long count();
}
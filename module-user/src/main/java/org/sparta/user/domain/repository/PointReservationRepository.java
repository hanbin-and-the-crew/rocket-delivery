package org.sparta.user.domain.repository;


import org.sparta.user.domain.entity.PointReservation;
import org.sparta.user.domain.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointReservationRepository extends JpaRepository<PointReservation, Long> {
    List<PointReservation> findByPaymentIdAndStatus(String paymentId, ReservationStatus status);
}
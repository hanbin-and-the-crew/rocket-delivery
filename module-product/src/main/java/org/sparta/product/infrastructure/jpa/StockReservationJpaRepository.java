package org.sparta.product.infrastructure.jpa;

import org.sparta.product.domain.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA 기반 재고 예약 저장소.
 */
public interface StockReservationJpaRepository extends JpaRepository<StockReservation, UUID> {

    Optional<StockReservation> findByReservationKey(String reservationKey);

    List<StockReservation> findAllByExternalReservationKey(String externalReservationKey);
}

package org.sparta.product.infrastructure.jpa;

import org.sparta.product.domain.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA 기반 재고 예약 저장소.
 *
 * - Spring Data JPA가 실제 구현체를 런타임에 자동으로 생성한다.
 * - infrastructure 계층에서만 사용하고, 도메인 계층에는 노출하지 않는다.
 */
public interface StockReservationJpaRepository extends JpaRepository<StockReservation, UUID> {

    Optional<StockReservation> findByReservationKey(String reservationKey);

}

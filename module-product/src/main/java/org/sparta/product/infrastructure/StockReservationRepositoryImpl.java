package org.sparta.product.infrastructure;

import lombok.RequiredArgsConstructor;
import org.sparta.product.domain.entity.StockReservation;
import org.sparta.product.domain.repository.StockReservationRepository;
import org.sparta.product.infrastructure.jpa.StockReservationJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 도메인 계층의 StockReservationRepository를
 * Spring Data JPA 기반 StockReservationJpaRepository로 구현한 어댑터.
 */
@Repository
@RequiredArgsConstructor
public class StockReservationRepositoryImpl implements StockReservationRepository {

    private final StockReservationJpaRepository jpaRepository;

    @Override
    public StockReservation save(StockReservation reservation) {
        return jpaRepository.save(reservation);
    }

    @Override
    public Optional<StockReservation> findByReservationKey(String reservationKey) {
        return jpaRepository.findByReservationKey(reservationKey);
    }
}

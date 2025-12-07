package org.sparta.product.domain.repository;

import org.sparta.product.domain.entity.StockReservation;

import java.util.Optional;

/**
 * 도메인 계층에서 사용하는 재고 예약 저장소 인터페이스.
 *
 * - 구현체는 infrastructure 계층에 위치하며, 여기서는 순수 도메인 관점의 연산만 정의한다.
 */
public interface StockReservationRepository {

    StockReservation save(StockReservation reservation);

    /**
     * 예약 키(reservationKey)로 예약 한 건을 조회.
     * - 주문 도메인이 넘겨준 키로 예약을 찾을 때 사용.
     */
    Optional<StockReservation> findByReservationKey(String reservationKey);

}

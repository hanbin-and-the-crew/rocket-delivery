package org.sparta.product.domain.repository;

import org.sparta.product.domain.entity.StockReservation;

import java.util.List;
import java.util.Optional;

/**
 * 도메인 계층에서 사용하는 재고 예약 저장소 인터페이스.
 */
public interface StockReservationRepository {

    StockReservation save(StockReservation reservation);

    /**
     * Product 내부 멱등 키(internalReservationKey = reservation_key)로 예약 1건 조회.
     */
    Optional<StockReservation> findByReservationKey(String reservationKey);

    /**
     * 외부 계약 키(externalReservationKey)로 해당 주문의 모든 예약 조회.
     * - confirm/cancel/recover 단계에서 외부에서 orderId 기반 키만 와도 일괄 처리가 가능해야 한다.
     */
    List<StockReservation> findAllByExternalReservationKey(String externalReservationKey);
}

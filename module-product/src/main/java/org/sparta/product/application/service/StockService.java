package org.sparta.product.application.service;

import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.error.BusinessException;
import org.sparta.product.domain.entity.Stock;
import org.sparta.product.domain.entity.StockReservation;
import org.sparta.product.domain.error.ProductErrorType;
import org.sparta.product.domain.repository.StockRepository;
import org.sparta.product.domain.repository.StockReservationRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Stock 서비스
 * - Product와 독립적으로 재고를 관리
 * - 낙관적 락 충돌 시 @Retryable로 자동 재시도
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockService {

    private final StockRepository stockRepository;
    private final StockReservationRepository stockReservationRepository;

    /**
     * Product ID로 재고 조회
     * - Stock과 Product는 독립된 ID를 가짐
     * - productId 필드로 연결
     */
    public Stock getStock(UUID productId) {
        return stockRepository.findByProductId(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorType.STOCK_NOT_FOUND));
    }

    /**
     * 재고 차감
     * - Stock만 독립적으로 조회
     * - Stock 애그리거트 내에서 차감 처리
     * - 낙관적 락 충돌 시 최대 3회 재시도 (50ms, 100ms, 150ms 간격)
     */
    @Retryable(
            retryFor = {OptimisticLockException.class, ObjectOptimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 50, multiplier = 2)
    )
    @Transactional
    public void decreaseStock(UUID productId, int quantity) {
        Stock stock = getStock(productId);
        stock.decrease(quantity);
    }

    /**
     * 재고 복원
     * - Stock만 독립적으로 조회
     * - Stock 애그리거트 내에서 복원 처리
     * - 낙관적 락 충돌 시 최대 3회 재시도 (50ms, 100ms, 150ms 간격)
     */
    @Retryable(
            retryFor = {OptimisticLockException.class, ObjectOptimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 50, multiplier = 2)
    )
    @Transactional
    public void increaseStock(UUID productId, int quantity) {
        Stock stock = getStock(productId);
        stock.increase(quantity);
    }



    /**
     * 재고 예약 (주문 생성 시)
     * - 주문 생성 시 호출
     * - reservationKey는 주문 도메인에서 생성한 멱등키 (현재 orderId.toString())
     * - 멱등성: 같은 reservationKey로 이미 예약이 있으면,
     *   - 수량이 같을 경우 그대로 반환
     *   - 수량이 다르면 예외 (정책상 안전하게 막는다)
     */
    @Retryable(
            retryFor = {OptimisticLockException.class, ObjectOptimisticLockingFailureException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 100, maxDelay = 1000, multiplier = 2, random = true)
    )
    @Transactional
    public StockReservation reserveStock(
            UUID productId,
            String reservationKey,
            int quantity) {

        log.info("[StockService] reserveStock called: productId={}, reservationKey={}, quantity={}",
                productId, reservationKey, quantity);

        // 1) 이미 동일 예약 키가 존재하면 멱등 처리
        return stockReservationRepository.findByReservationKey(reservationKey)
                .map(existing -> {
                    // 수량이 다르면 정책상 예외
                    if (existing.getReservedQuantity() != quantity) {
                        throw new BusinessException(ProductErrorType.STOCK_RESERVATION_ALREADY_EXISTS);
                    }

                    log.debug("[StockService] idempotent reserve: reservationKey={}, quantity={}",
                            reservationKey, quantity);

                    return existing;
                })
                .orElseGet(() -> {
                    // 2) 재고 조회
                    Stock stock = getStock(productId);

                    log.debug("[StockService] creating new reservation: productId={}, reservationKey={}, requestedQuantity={}, availableQuantity={}",
                            productId, reservationKey, quantity, stock.getAvailableQuantity());

                    // 3) Stock 애그리게이트에서 예약 수행 (재고 부족/상태 이상 시 BusinessException 발생)
                    stock.reserve(quantity);

                    // 4) StockReservation 엔티티 생성 및 저장
                    StockReservation reservation = StockReservation.create(
                            stock.getId(),
                            reservationKey,
                            quantity
                    );
                    stockReservationRepository.save(reservation);

                    // 5) 변경된 Stock 저장
                    stockRepository.save(stock);

                    log.info("[StockService] reservation created: reservationId={}, stockId={}, reservationKey={}, reservedQuantity={}, remainingAvailableQuantity={}",
                            reservation.getId(), reservation.getStockId(), reservation.getReservationKey(),
                            reservation.getReservedQuantity(), stock.getAvailableQuantity());

                    return reservation;
                });
    }

    /**
     * 예약 확정 (결제 완료 시)
     * - reservationKey 기반으로 예약 한 건을 조회
     * - 예약 상태를 CONFIRMED로 변경
     * - Stock 애그리게이트에서 실제 재고 차감(confirmReservation) 수행
     * - 멱등성: 이미 CONFIRMED 상태이면 조용히 반환
     */
    @Retryable(
            retryFor = {OptimisticLockException.class, ObjectOptimisticLockingFailureException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 100, maxDelay = 1000, multiplier = 2, random = true)
    )
    @Transactional
    public void confirmReservation(String reservationKey) {

        log.info("[StockService] confirmReservation called: reservationKey={}", reservationKey);

        // 1) 예약 조회
        StockReservation reservation = stockReservationRepository.findByReservationKey(reservationKey)
                .orElseThrow(() -> new BusinessException(ProductErrorType.STOCK_RESERVATION_NOT_FOUND));

        // 2) 멱등성 처리
        if (reservation.isConfirmed()) {

            // 이미 확정된 예약이면 종료 (멱등)
            log.debug("[StockService] confirmReservation idempotent: already CONFIRMED, reservationKey={}", reservationKey);
            return;
        }
        if (reservation.isCancelled()) {
            // 취소된 예약을 확정하려는 잘못된 흐름
            log.warn("[StockService] confirmReservation invalid state: already CANCELLED, reservationKey={}", reservationKey);
            throw new BusinessException(ProductErrorType.STOCK_RESERVATION_ALREADY_CANCELLED);
        }

        // 3) Stock 조회
        Stock stock = stockRepository.findById(reservation.getStockId())
                .orElseThrow(() -> new BusinessException(ProductErrorType.STOCK_NOT_FOUND));

        int quantityToConfirm = reservation.getReservedQuantity();

        // 4) 예약 확정 → 실제 재고 차감
        stock.confirmReservation(quantityToConfirm);

        // 5) Reservation 상태 변경
        reservation.confirm();

        // 6) 저장
        stockRepository.save(stock);
        stockReservationRepository.save(reservation);

        log.info("[StockService] confirmReservation completed: reservationKey={}, confirmedQuantity={}, remainingAvailableQuantity={}",
                reservationKey, quantityToConfirm, stock.getAvailableQuantity());
    }


    /**
     * 재고 예약 취소 (주문 취소 / 결제 실패 시)
     * - reservationKey 기반으로 예약 한 건을 조회
     * - 예약 상태를 CANCELLED로 변경
     * - Stock 애그리게이트에서 예약 수량만 되돌림(cancelReservation)
     * - 멱등성: 이미 CANCELLED 상태이면 조용히 반환
     */
    @Retryable(
            retryFor = {OptimisticLockException.class, ObjectOptimisticLockingFailureException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 100, maxDelay = 1000, multiplier = 2, random = true)
    )
    @Transactional
    public void cancelReservation(String reservationKey) {
        log.info("[StockService] cancelReservation called: reservationKey={}", reservationKey);

        // 1) 예약 조회
        StockReservation reservation = stockReservationRepository.findByReservationKey(reservationKey)
                .orElseThrow(() -> new BusinessException(ProductErrorType.STOCK_RESERVATION_NOT_FOUND));

        // 2) 멱등성 처리
        if (reservation.isCancelled()) {
            // 이미 취소된 예약이면 종료 (멱등)
            log.debug("[StockService] cancelReservation idempotent: already CANCELLED, reservationKey={}", reservationKey);
            return;
        }
        if (reservation.isConfirmed()) {
            // 이미 확정된 예약을 취소하려면, 별도의 반품/롤백 프로세스가 필요
            log.warn("[StockService] cancelReservation invalid state: already CONFIRMED, reservationKey={}", reservationKey);
            throw new BusinessException(ProductErrorType.STOCK_RESERVATION_ALREADY_CONFIRMED);
        }

        // 3) Stock 조회
        Stock stock = stockRepository.findById(reservation.getStockId())
                .orElseThrow(() -> new BusinessException(ProductErrorType.STOCK_NOT_FOUND));

        int quantityToCancel = reservation.getReservedQuantity();

        // 4) 예약 취소 → 예약 수량만 되돌림
        stock.cancelReservation(quantityToCancel);

        // 5) Reservation 상태 변경
        reservation.cancel();

        // 6) 저장
        stockRepository.save(stock);
        stockReservationRepository.save(reservation);

        log.info("[StockService] cancelReservation completed: reservationKey={}, cancelledQuantity={}, remainingAvailableQuantity={}",
                reservationKey, quantityToCancel, stock.getAvailableQuantity());
    }

}

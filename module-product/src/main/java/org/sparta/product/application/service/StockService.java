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
import org.sparta.product.domain.util.ReservationKeyUtil;
import org.sparta.redis.util.DistributedLockExecutor;
import org.sparta.redis.util.LockAcquisitionException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 재고 예약/확정/취소 서비스
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockService {

    private final StockRepository stockRepository;
    private final StockReservationRepository stockReservationRepository;
    private final DistributedLockExecutor lockExecutor;

    /**
     * 재고 예약
     *
     * externalReservationKey(예: orderId.toString())는 외부 계약 그대로 받고,
     * 내부 저장/멱등용 키(internalReservationKey)를 파생해 저장한다.
     */
    @Transactional
    @Retryable(
            retryFor = {
                    OptimisticLockException.class,
                    ObjectOptimisticLockingFailureException.class
            },
            maxAttempts = 5,
            backoff = @Backoff(delay = 30, multiplier = 2)
    )
    public StockReservation reserveStock(UUID productId, String externalReservationKey, int quantity) {

        if (productId == null) {
            throw new BusinessException(ProductErrorType.PRODUCT_REQUIRED);
        }
        if (externalReservationKey == null || externalReservationKey.isBlank()) {
            throw new BusinessException(ProductErrorType.STOCK_RESERVATION_KEY_REQUIRED);
        }
        if (quantity < 1) {
            throw new BusinessException(ProductErrorType.RESERVE_QUANTITY_INVALID);
        }

        final String internalReservationKey = ReservationKeyUtil.internalKey(externalReservationKey, productId);

        return executeWithLock("product:stock:lock:" + productId, () -> {
            // 1) internalReservationKey 멱등 처리
            StockReservation existing = stockReservationRepository.findByReservationKey(internalReservationKey).orElse(null);
            if (existing != null) {
                Stock existingStock = stockRepository.findById(existing.getStockId())
                        .orElseThrow(() -> new BusinessException(ProductErrorType.STOCK_NOT_FOUND));

                if (!existingStock.getProductId().equals(productId)) {
                    throw new BusinessException(ProductErrorType.STOCK_RESERVATION_CONFLICT);
                }

                // 수량이 다르면 같은 키로 서로 다른 예약을 걸려는 시도 => 충돌
                if (existing.getReservedQuantity() != quantity) {
                    throw new BusinessException(ProductErrorType.STOCK_RESERVATION_ALREADY_EXISTS);
                }

                // 외부키가 다르면 데이터 이상 => 충돌
                if (!Objects.equals(existing.getExternalReservationKey(), externalReservationKey)) {
                    throw new BusinessException(ProductErrorType.STOCK_RESERVATION_CONFLICT);
                }

                return existing;
            }

            // 2) 신규 예약
            Stock stock = stockRepository.findByProductId(productId)
                    .orElseThrow(() -> new BusinessException(ProductErrorType.STOCK_NOT_FOUND));

            stock.reserve(quantity);
            StockReservation reservation = StockReservation.reserve(
                    stock.getId(),
                    externalReservationKey,
                    internalReservationKey,
                    quantity
            );

            stockRepository.save(stock);
            stockReservationRepository.save(reservation);

            return reservation;
        });
    }

    /**
     * 예약 확정(실차감)
     *
     * - inputKey가 internalReservationKey면 단건 처리
     * - inputKey가 externalReservationKey(orderId 기반)면 해당 주문의 모든 예약을 일괄 처리
     */
    @Transactional
    @Retryable(
            retryFor = {
                    OptimisticLockException.class,
                    ObjectOptimisticLockingFailureException.class
            },
            maxAttempts = 5,
            backoff = @Backoff(delay = 30, multiplier = 2)
    )
    public void confirmReservation(String inputKey) {
        if (inputKey == null || inputKey.isBlank()) {
            throw new BusinessException(ProductErrorType.STOCK_RESERVATION_KEY_REQUIRED);
        }

        // 1) externalReservationKey로 전체 조회(운영 편의)
        List<StockReservation> byExternal = stockReservationRepository.findAllByExternalReservationKey(inputKey);
        if (!byExternal.isEmpty()) {
            for (StockReservation r : byExternal) {
                confirmOne(r.getReservationKey());
            }
            return;
        }

        // 2) 없으면 internalReservationKey 단건 처리(호환)
        confirmOne(inputKey);
    }

    private void confirmOne(String internalReservationKey) {

        StockReservation reservation = stockReservationRepository.findByReservationKey(internalReservationKey)
                .orElseThrow(() -> new BusinessException(ProductErrorType.STOCK_RESERVATION_NOT_FOUND));

        executeWithLock("product:stock:lock:" + reservation.getStockId(), () -> {
            StockReservation fresh = stockReservationRepository.findByReservationKey(internalReservationKey)
                    .orElseThrow(() -> new BusinessException(ProductErrorType.STOCK_RESERVATION_NOT_FOUND));

            if (fresh.isConfirmed()) {
                return null; // 멱등
            }
            if (fresh.isCancelled()) {
                throw new BusinessException(ProductErrorType.STOCK_RESERVATION_ALREADY_CANCELLED);
            }

            Stock stock = stockRepository.findById(fresh.getStockId())
                    .orElseThrow(() -> new BusinessException(ProductErrorType.STOCK_NOT_FOUND));

            stock.confirmReservation(fresh.getReservedQuantity());
            fresh.confirm();

            stockRepository.save(stock);
            stockReservationRepository.save(fresh);

            return null;
        });
    }

    /**
     * 예약 취소(복구)
     *
     * - inputKey가 internalReservationKey면 단건 처리
     * - inputKey가 externalReservationKey(orderId 기반)면 해당 주문의 모든 예약을 일괄 처리
     */
    @Transactional
    @Retryable(
            retryFor = {
                    OptimisticLockException.class,
                    ObjectOptimisticLockingFailureException.class
            },
            maxAttempts = 5,
            backoff = @Backoff(delay = 30, multiplier = 2)
    )
    public void cancelReservation(String inputKey) {
        if (inputKey == null || inputKey.isBlank()) {
            throw new BusinessException(ProductErrorType.STOCK_RESERVATION_KEY_REQUIRED);
        }

        List<StockReservation> byExternal = stockReservationRepository.findAllByExternalReservationKey(inputKey);
        if (!byExternal.isEmpty()) {
            for (StockReservation r : byExternal) {
                cancelOne(r.getReservationKey());
            }
            return;
        }

        cancelOne(inputKey);
    }

    private void cancelOne(String internalReservationKey) {

        StockReservation reservation = stockReservationRepository.findByReservationKey(internalReservationKey)
                .orElseThrow(() -> new BusinessException(ProductErrorType.STOCK_RESERVATION_NOT_FOUND));

        executeWithLock("product:stock:lock:" + reservation.getStockId(), () -> {
            StockReservation fresh = stockReservationRepository.findByReservationKey(internalReservationKey)
                    .orElseThrow(() -> new BusinessException(ProductErrorType.STOCK_RESERVATION_NOT_FOUND));

            if (fresh.isCancelled()) {
                return null; // 멱등
            }
            if (fresh.isConfirmed()) {
                throw new BusinessException(ProductErrorType.STOCK_RESERVATION_ALREADY_CONFIRMED);
            }

            Stock stock = stockRepository.findById(fresh.getStockId())
                    .orElseThrow(() -> new BusinessException(ProductErrorType.STOCK_NOT_FOUND));

            stock.cancelReservation(fresh.getReservedQuantity());
            fresh.cancel();

            stockRepository.save(stock);
            stockReservationRepository.save(fresh);

            return null;
        });
    }

    @Transactional
    public void compensateOrderCancellation(String externalReservationKey) {
        if (externalReservationKey == null || externalReservationKey.isBlank()) {
            throw new BusinessException(ProductErrorType.STOCK_RESERVATION_KEY_REQUIRED);
        }

        List<StockReservation> reservations =
                stockReservationRepository.findAllByExternalReservationKey(externalReservationKey);

        if (reservations.isEmpty()) {
            return;
        }

        for (StockReservation reservation : reservations) {

            if (reservation.isCancelled()) {
                continue; // 멱등
            }

            // RESERVED 판단: confirmed도 아니고 cancelled도 아닌 상태
            if (!reservation.isConfirmed()) {
                // 기존 cancel 흐름 재사용(= 재고 복구 + reservation.cancel + 저장까지 포함)
                cancelOne(reservation.getReservationKey());
                continue;
            }

            // CONFIRMED 보상
            compensateConfirmedOne(reservation.getReservationKey());
        }
    }

    private void compensateConfirmedOne(String internalReservationKey) {

        StockReservation reservation = stockReservationRepository.findByReservationKey(internalReservationKey)
                .orElseThrow(() -> new BusinessException(ProductErrorType.STOCK_RESERVATION_NOT_FOUND));

        executeWithLock("product:stock:lock:" + reservation.getStockId(), () -> {

            StockReservation fresh = stockReservationRepository.findByReservationKey(internalReservationKey)
                    .orElseThrow(() -> new BusinessException(ProductErrorType.STOCK_RESERVATION_NOT_FOUND));

            if (fresh.isCancelled()) {
                return null; // 멱등
            }

            // CONFIRMED만 보상 대상으로 처리
            if (!fresh.isConfirmed()) {
                return null;
            }

            Stock stock = stockRepository.findById(fresh.getStockId())
                    .orElseThrow(() -> new BusinessException(ProductErrorType.STOCK_NOT_FOUND));

            stock.restoreConfirmedReservation(fresh.getReservedQuantity());
            fresh.compensateCancel();

            stockRepository.save(stock);
            stockReservationRepository.save(fresh);

            return null;
        });
    }



    private <T> T executeWithLock(String lockKey, Supplier<T> action) {
        try {
            return lockExecutor.executeWithLock(lockKey, 0, 8, TimeUnit.SECONDS, action);
        } catch (LockAcquisitionException ex) {
            throw new BusinessException(ProductErrorType.STOCK_LOCK_BUSY);
        }
    }
}

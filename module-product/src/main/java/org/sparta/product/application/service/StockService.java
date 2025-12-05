package org.sparta.product.application.service;

import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.error.BusinessException;
import org.sparta.product.domain.entity.Stock;
import org.sparta.product.domain.error.ProductErrorType;
import org.sparta.product.domain.repository.StockRepository;
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
     * - 가용 재고 확인 후 예약
     * - Stock 애그리거트 내에서 예약 처리
     * - 낙관적 락 충돌 시 최대 5회 재시도 (랜덤 백오프로 경합 감소)
     */
    @Retryable(
            retryFor = {OptimisticLockException.class, ObjectOptimisticLockingFailureException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 100, maxDelay = 1000, multiplier = 2, random = true)
    )
    @Transactional
    public void reserveStock(UUID productId, int quantity) {
        Stock stock = getStock(productId);
        stock.reserve(quantity);
    }

    /**
     * 예약 확정 (결제 완료 시)
     * - 예약된 재고를 실제 차감
     * - Stock 애그리거트 내에서 확정 처리
     * - 낙관적 락 충돌 시 최대 5회 재시도 (랜덤 백오프로 경합 감소)
     */
    @Retryable(
            retryFor = {OptimisticLockException.class, ObjectOptimisticLockingFailureException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 100, maxDelay = 1000, multiplier = 2, random = true)
    )
    @Transactional
    public void confirmReservation(UUID productId, int quantity) {
        Stock stock = getStock(productId);
        stock.confirmReservation(quantity);
    }

    /**
     * 예약 취소 (주문 취소 시)
     * - 예약된 재고만 감소
     * - Stock 애그리거트 내에서 취소 처리
     * - 낙관적 락 충돌 시 최대 5회 재시도 (랜덤 백오프로 경합 감소)
     */
    @Retryable(
            retryFor = {OptimisticLockException.class, ObjectOptimisticLockingFailureException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 100, maxDelay = 1000, multiplier = 2, random = true)
    )
    @Transactional
    public void cancelReservation(UUID productId, int quantity) {
        Stock stock = getStock(productId);
        stock.cancelReservation(quantity);
    }
}

package org.sparta.product.application.service;

import lombok.RequiredArgsConstructor;
import org.sparta.common.error.BusinessException;
import org.sparta.product.domain.entity.Stock;
import org.sparta.product.domain.error.ProductErrorType;
import org.sparta.product.domain.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Stock 서비스
 * - Product와 독립적으로 재고를 관리
 * - Product 더티체킹을 방지하기 위해 Stock만 조회/수정
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockService {

    private final StockRepository stockRepository;

    /**
     * 재고 조회
     */
    public Stock getStock(UUID productId) {
        return stockRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorType.STOCK_NOT_FOUND));
    }

    /**
     * 재고 차감
     * - Stock만 독립적으로 조회
     * - Stock 애그리거트 내에서 차감 처리
     */
    @Transactional
    public void decreaseStock(UUID productId, int quantity) {
        Stock stock = getStock(productId);
        stock.decrease(quantity);
    }

    /**
     * 재고 복원
     * - Stock만 독립적으로 조회
     * - Stock 애그리거트 내에서 복원 처리
     */
    @Transactional
    public void increaseStock(UUID productId, int quantity) {
        Stock stock = getStock(productId);
        stock.increase(quantity);
    }
}

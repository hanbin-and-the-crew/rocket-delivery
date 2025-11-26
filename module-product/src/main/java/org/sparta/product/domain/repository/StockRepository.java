package org.sparta.product.domain.repository;

import org.sparta.product.domain.entity.Stock;

import java.util.Optional;
import java.util.UUID;

/**
 * Stock Repository 인터페이스
 * - Product와 독립적으로 Stock 조회/수정
 */
public interface StockRepository {

    /**
     * Stock ID로 조회
     */
    Optional<Stock> findById(UUID stockId);

    /**
     * Product ID로 Stock 조회
     * - Stock이 productId 필드를 통해 Product와 연결됨
     */
    Optional<Stock> findByProductId(UUID productId);

    /**
     * Stock 저장
     */
    Stock save(Stock stock);
}

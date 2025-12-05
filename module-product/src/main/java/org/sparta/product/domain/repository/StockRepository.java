package org.sparta.product.domain.repository;

import org.sparta.product.domain.entity.Stock;

import java.util.Optional;
import java.util.UUID;

/**
 * Product와 독립적으로 Stock 조회/수정
 */
public interface StockRepository {


    Optional<Stock> findById(UUID stockId);

    /**
     * Product ID로 Stock 조회
     * - Stock이 productId 필드를 통해 Product와 연결됨
     */
    Optional<Stock> findByProductId(UUID productId);

    Stock save(Stock stock);
}

package org.sparta.order.domain.repository;

import org.sparta.order.domain.entity.Stock;

import java.util.Optional;
import java.util.UUID;

/**
 * Stock Repository 인터페이스
 * - Product 더티체킹 방지를 위해 Stock 독립적으로 조회/수정
 */
public interface StockRepository {

    /**
     * Product ID로 Stock 조회
     * - @MapsId로 Product ID와 Stock ID가 동일
     */
    Optional<Stock> findById(UUID productId);
    Stock save(Stock stock);
}

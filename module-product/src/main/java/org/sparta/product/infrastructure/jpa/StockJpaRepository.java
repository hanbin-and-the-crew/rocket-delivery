package org.sparta.product.infrastructure.jpa;

import org.sparta.product.domain.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Stock Repository
 * - Product와 독립적으로 재고 관리
 * - Product 더티체킹 없이 Stock만 수정 가능
 * - 낙관적 락으로 동시성 제어
 */
public interface StockJpaRepository extends JpaRepository<Stock, UUID> {

    /**
     * Product ID로 Stock 조회
     */
    java.util.Optional<Stock> findByProductId(UUID productId);
}
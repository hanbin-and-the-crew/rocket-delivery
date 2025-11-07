package org.sparta.product.infrastructure.jpa;

import org.sparta.product.domain.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Product Repository
 * - Product 메타데이터 조회/저장
 * - Stock은 StockRepository를 통해 독립적으로 관리
 */
public interface ProductJpaRepository extends JpaRepository<Product, UUID> {

}
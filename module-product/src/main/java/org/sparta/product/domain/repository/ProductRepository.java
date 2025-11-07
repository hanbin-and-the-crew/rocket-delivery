package org.sparta.product.domain.repository;

import org.sparta.product.domain.entity.Product;

import java.util.Optional;
import java.util.UUID;

/**
 * Product Repository 인터페이스
 */
public interface ProductRepository {

    /**
     * 상품 저장
     */
    Product save(Product product);

    /**
     * 상품 ID로 조회
     */
    Optional<Product> findById(UUID id);
}

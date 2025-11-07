package org.sparta.product.infrastructure;

import lombok.RequiredArgsConstructor;
import org.sparta.product.domain.entity.Product;
import org.sparta.product.domain.repository.ProductRepository;
import org.sparta.product.infrastructure.jpa.ProductJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository productJpaRepository;

    @Override
    public Product save(Product product) {
        return productJpaRepository.save(product);
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return productJpaRepository.findById(id);
    }
}

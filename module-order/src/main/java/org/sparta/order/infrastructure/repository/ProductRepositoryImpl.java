package org.sparta.order.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.sparta.order.domain.entity.Product;
import org.sparta.order.domain.repository.ProductRepository;
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

    @Override
    public void deleteAll() {
        productJpaRepository.deleteAll();
    }
}

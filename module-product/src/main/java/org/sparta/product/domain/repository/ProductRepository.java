package org.sparta.product.domain.repository;

import org.sparta.product.domain.entity.Product;

import java.util.Optional;
import java.util.UUID;


public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(UUID id);

    void deleteAll();
}

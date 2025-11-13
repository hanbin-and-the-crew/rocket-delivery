package org.sparta.product.infrastructure.jpa;

import org.sparta.product.domain.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;


public interface CategoryJpaRepository extends JpaRepository<Category, UUID> {

    boolean existsById(UUID id);
}
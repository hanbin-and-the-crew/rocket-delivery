package org.sparta.product.infrastructure.jpa;

import org.sparta.product.domain.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;


public interface CategoryJpaRepository extends JpaRepository<Category, Long> {
}
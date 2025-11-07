package org.sparta.product.infrastructure;

import lombok.RequiredArgsConstructor;
import org.sparta.product.domain.repository.CategoryRepository;
import org.sparta.product.infrastructure.jpa.CategoryJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CategoryRepositoryImpl implements CategoryRepository {

    private CategoryJpaRepository categoryJpaRepository;

    @Override
    public boolean existsById(UUID categoryId) {
        return categoryJpaRepository.existsById(categoryId);
    }
}

package org.sparta.product.infrastructure;

import lombok.RequiredArgsConstructor;
import org.sparta.product.domain.entity.Category;
import org.sparta.product.domain.repository.CategoryRepository;
import org.sparta.product.infrastructure.jpa.CategoryJpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CategoryRepositoryImpl implements CategoryRepository {

    private final CategoryJpaRepository categoryJpaRepository;

    @Override
    public boolean existsById(UUID categoryId) {
        return categoryJpaRepository.existsById(categoryId);
    }

    @Override
    public Category save(Category category) {
        return categoryJpaRepository.save(category);
    }

    @Override
    public void deleteAll() {
        categoryJpaRepository.deleteAll();
    }

    @Override
    public long count() {
        return categoryJpaRepository.count();
    }

    @Override
    public List<Category> findAll() {
        return categoryJpaRepository.findAll();
    }
}

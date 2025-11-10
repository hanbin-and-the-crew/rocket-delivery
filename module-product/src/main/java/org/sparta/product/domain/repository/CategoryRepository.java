package org.sparta.product.domain.repository;

import org.sparta.product.domain.entity.Category;

import java.util.UUID;

public interface CategoryRepository {

    boolean existsById(UUID categoryId);

    Category save(Category category);

    void deleteAll();
}

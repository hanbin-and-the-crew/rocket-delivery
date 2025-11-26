package org.sparta.product.domain.repository;

import org.sparta.product.domain.entity.Category;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository {

    boolean existsById(UUID categoryId);

    Category save(Category category);

    void deleteAll();

    long count();

    List<Category> findAll();
}

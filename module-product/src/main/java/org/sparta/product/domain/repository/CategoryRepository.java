package org.sparta.product.domain.repository;

import java.util.UUID;

public interface CategoryRepository {

    boolean existsById(UUID categoryId);
}

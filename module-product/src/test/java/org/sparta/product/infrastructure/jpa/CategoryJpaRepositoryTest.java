package org.sparta.product.infrastructure.jpa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class CategoryJpaRepositoryTest {

    @Autowired
    private CategoryJpaRepository categoryJpaRepository;

    @Test
    @DisplayName("CategoryJpaRepository.existsById: 저장된 엔티티에 대해 true/없으면 false")
    void existsById_works() {
        assertFalse(categoryJpaRepository.existsById(UUID.randomUUID()));
    }
}

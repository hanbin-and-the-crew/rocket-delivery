package org.sparta.product.infrastructure.jpa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.product.domain.entity.Product;
import org.sparta.product.domain.vo.Money;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ProductJpaRepositoryTest {

    @Autowired
    private ProductJpaRepository repository;

    @Test
    @DisplayName("Product 저장 후 findById 조회 가능")
    void save_and_findById() {
        Product product = Product.create(
                "상품",
                Money.of(1000L),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                10
        );

        Product saved = repository.save(product);

        assertThat(repository.findById(saved.getId())).isPresent();
    }
}

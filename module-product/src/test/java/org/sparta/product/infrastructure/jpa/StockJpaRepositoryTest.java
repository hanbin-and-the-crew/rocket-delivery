package org.sparta.product.infrastructure.jpa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.product.domain.entity.Stock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class StockJpaRepositoryTest {

    @Autowired
    private StockJpaRepository repository;

    @Test
    @DisplayName("productId로 Stock 조회 가능")
    void findByProductId() {
        UUID productId = UUID.randomUUID();

        Stock stock = Stock.create(
                productId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                50
        );

        repository.save(stock);

        assertThat(repository.findByProductId(productId)).isPresent();
    }
}

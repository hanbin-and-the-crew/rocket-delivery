package org.sparta.product.infrastructure.jpa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.product.domain.entity.Stock;
import org.sparta.product.domain.enums.StockStatus;
import org.sparta.product.infrastructure.StockReservationRepositoryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * StockJpaRepository JPA 매핑 테스트
 */
@DataJpaTest
@ActiveProfiles("test")
class StockJpaRepositoryTest {

    @Autowired
    private StockJpaRepository stockJpaRepository;

    @Autowired
    private TestEntityManager entityManager;


    @MockBean
    private StockReservationRepositoryImpl StockReservationRepositoryImpl;

    @Test
    @DisplayName("Stock을 저장하고 ID로 조회할 수 있다")
    void saveAndFindById() {
        // given
        UUID productId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        Stock stock = Stock.create(
                productId,
                companyId,
                hubId,
                100
        );

        // when
        Stock saved = stockJpaRepository.save(stock);

        entityManager.flush();
        entityManager.clear();

        Stock found = stockJpaRepository.findById(saved.getId())
                .orElseThrow(() -> new AssertionError("Stock이 조회되지 않았습니다."));

        // then
        assertThat(found.getId()).isNotNull();
        assertThat(found.getProductId()).isEqualTo(productId);
        assertThat(found.getCompanyId()).isEqualTo(companyId);
        assertThat(found.getHubId()).isEqualTo(hubId);
        assertThat(found.getQuantity()).isEqualTo(100);
        assertThat(found.getReservedQuantity()).isZero();
        assertThat(found.getStatus()).isEqualTo(StockStatus.IN_STOCK);
    }

    @Test
    @DisplayName("productId로 Stock을 조회할 수 있다")
    void findByProductId() {
        // given
        UUID productId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        Stock stock = Stock.create(
                productId,
                companyId,
                hubId,
                50
        );

        stockJpaRepository.save(stock);
        entityManager.flush();
        entityManager.clear();

        // when
        Optional<Stock> optional = stockJpaRepository.findByProductId(productId);

        // then
        assertThat(optional).isPresent();
        Stock found = optional.get();
        assertThat(found.getProductId()).isEqualTo(productId);
        assertThat(found.getQuantity()).isEqualTo(50);
    }

    @Test
    @DisplayName("productId에 해당하는 Stock이 없으면 Optional.empty를 반환한다")
    void findByProductId_WhenNotExists_ShouldReturnEmpty() {
        // given
        UUID notExistingProductId = UUID.randomUUID();

        // when
        Optional<Stock> optional = stockJpaRepository.findByProductId(notExistingProductId);

        // then
        assertThat(optional).isEmpty();
    }
}

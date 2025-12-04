package org.sparta.product.infrastructure.jpa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.product.domain.entity.Product;
import org.sparta.product.domain.vo.Money;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ProductJpaRepository JPA 매핑 테스트
 *
 * - Product 엔티티와 JPA 매핑이 정상 동작하는지 검증한다.
 * - 이 테스트에서는 ProductJpaRepository만 필요하므로,
 *   순환 참조를 일으키는 다른 인프라 빈(StockReservationJpaRepositoryImpl)은
 *   @MockBean 으로 대체해서 컨텍스트 로딩 에러를 막는다.
 */
@DataJpaTest
@ActiveProfiles("test")
class ProductJpaRepositoryTest {

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private TestEntityManager entityManager;

    /**
     * DataJpaTest가 module-product 안의 @Repository 들을 전부 스캔하면서
     * StockReservationJpaRepositoryImpl 빈 생성 중 순환 참조 에러가 발생했었기 때문에,
     * 이 테스트에서는 해당 빈을 MockBean 으로 등록해서 실제 구현을 생성하지 않도록 한다.
     */
    @MockBean
    private org.sparta.product.infrastructure.jpa.StockReservationJpaRepositoryImpl stockReservationJpaRepositoryImpl;

    @Test
    @DisplayName("Product를 저장하고 ID로 조회할 수 있다")
    void saveAndFindById() {
        // given
        UUID categoryId = UUID.randomUUID();
        UUID companyId  = UUID.randomUUID();
        UUID hubId      = UUID.randomUUID();

        String productName = "JPA 테스트 상품";
        long price = 15_000L;
        int initialQuantity = 100;

        // 도메인 팩토리 메서드로 Product 생성
        Product product = Product.create(
                productName,
                Money.of(price),
                categoryId,
                companyId,
                hubId,
                initialQuantity
        );

        // when
        Product saved = productJpaRepository.save(product);

        // 1차 캐시 비워서 실제 DB에서 다시 가져오도록 강제
        entityManager.flush();
        entityManager.clear();

        Product found = productJpaRepository.findById(saved.getId())
                .orElseThrow(() -> new AssertionError("Product가 조회되지 않았습니다."));

        // then
        assertThat(found.getId()).isNotNull();
        assertThat(found.getProductName()).isEqualTo(productName);
        assertThat(found.getPrice().getAmount()).isEqualTo(price);
        assertThat(found.getCategoryId()).isEqualTo(categoryId);
        assertThat(found.getCompanyId()).isEqualTo(companyId);
        assertThat(found.getHubId()).isEqualTo(hubId);
        assertThat(found.getIsActive()).isTrue(); // 기본 활성 상태 매핑 확인
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회하면 Optional.empty를 반환한다")
    void findById_WhenNotExists_ShouldReturnEmpty() {
        // given
        UUID notExistingId = UUID.randomUUID();

        // when
        Optional<Product> optional = productJpaRepository.findById(notExistingId);

        // then
        assertThat(optional).isEmpty();
    }
}

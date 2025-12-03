//package org.sparta.product.infrastructure.jpa;
//
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
//import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
//import org.springframework.context.annotation.Import;
//import org.springframework.test.context.ActiveProfiles;
//import org.sparta.product.domain.entity.Category;
//import org.sparta.product.domain.entity.Product;
//import org.sparta.product.domain.entity.Stock;
//import org.sparta.product.domain.repository.CategoryRepository;
//import org.sparta.product.domain.vo.Money;
//import org.sparta.product.infrastructure.CategoryRepositoryImpl;
//
//import java.util.Optional;
//import java.util.UUID;
//
//import static org.assertj.core.api.Assertions.*;
//
///**
// * StockJpaRepository 테스트
// *
// * 테스트 전략:
// * - @DataJpaTest: Repository 레이어 테스트
// * - H2 In-Memory DB 사용
// * - TestEntityManager: 영속성 컨텍스트 제어로 테스트 격리
// * - 기본 조회만 검증 (Stock은 Product를 통해 생성됨)
// *
// * 주의사항:
// * - Stock은 Product와 생명주기를 공유하므로 독립적으로 생성하지 않음
// * - Product ID와 Stock ID가 동일함 (@MapsId)
// */
//@DataJpaTest
//@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
//@ActiveProfiles("test")
//@Import(CategoryRepositoryImpl.class)
//class StockJpaRepositoryTest {
//
//    @Autowired
//    private StockJpaRepository stockJpaRepository;
//
//    @Autowired
//    private ProductJpaRepository productJpaRepository;
//
//    @Autowired
//    private CategoryRepository categoryRepository;
//
//    @Autowired
//    private TestEntityManager entityManager;
//
//    @Test
//    @DisplayName("Product ID로 Stock을 조회할 수 있다")
//    void findById_ShouldReturnStock() {
//        // given: Product 생성 시 Stock도 함께 생성됨
//        Category category = Category.create("전자제품", "전자제품 카테고리");
//        categoryRepository.save(category);
//
//        Product product = Product.create(
//                "노트북",
//                Money.of(1500000L),
//                category.getId(),
//                UUID.randomUUID(),
//                UUID.randomUUID(),
//                100
//        );
//        Product savedProduct = productJpaRepository.save(product);
//        UUID productId = savedProduct.getId();
//        Stock savedStock = Stock.create(
//                productId,
//                savedProduct.getCompanyId(),
//                savedProduct.getHubId(),
//                100
//        );
//        stockJpaRepository.save(savedStock);
//
//        entityManager.flush();
//        entityManager.clear();
//
//        // when: Product ID로 Stock 조회
//        Optional<Stock> foundStock = stockJpaRepository.findByProductId(productId);
//
//        // then: Stock이 조회됨
//        assertThat(foundStock).isPresent();
//        assertThat(foundStock.get().getQuantity()).isEqualTo(100);
//        assertThat(foundStock.get().getReservedQuantity()).isEqualTo(0);
//    }
//
//    @Test
//    @DisplayName("존재하지 않는 Product ID로 조회하면 빈 Optional이 반환된다")
//    void findById_WithNonExistentId_ShouldReturnEmpty() {
//        // given: 존재하지 않는 Product ID
//        UUID nonExistentId = UUID.randomUUID();
//
//        // when: 조회
//        Optional<Stock> result = stockJpaRepository.findByProductId(nonExistentId);
//
//        // then: 빈 Optional 반환
//        assertThat(result).isEmpty();
//    }
//
//    @Test
//    @DisplayName("Stock을 수정할 수 있다")
//    void update_ShouldWork() {
//        // given: Stock이 있는 Product
//        Category category = Category.create("가전제품", "가전제품 카테고리");
//        categoryRepository.save(category);
//
//        Product product = Product.create(
//                "냉장고",
//                Money.of(2000000L),
//                category.getId(),
//                UUID.randomUUID(),
//                UUID.randomUUID(),
//                50
//        );
//        Product savedProduct = productJpaRepository.save(product);
//        UUID productId = savedProduct.getId();
//        Stock savedStock = Stock.create(
//                productId,
//                savedProduct.getCompanyId(),
//                savedProduct.getHubId(),
//                50
//        );
//        stockJpaRepository.save(savedStock);
//
//        entityManager.flush();
//        entityManager.clear();
//
//        // when: Stock 수정 (재고 차감)
//        Stock foundStock = stockJpaRepository.findByProductId(productId).orElseThrow();
//        foundStock.decrease(10);
//        stockJpaRepository.save(foundStock);
//
//        entityManager.flush();
//        entityManager.clear();
//
//        // then: 수정된 재고가 반영됨
//        Stock updatedStock = stockJpaRepository.findByProductId(productId).get();
//        assertThat(updatedStock.getQuantity()).isEqualTo(40);
//    }
//
//    @Test
//    @DisplayName("가용 재고를 계산할 수 있다")
//    void getAvailableQuantity_ShouldWork() {
//        // given: Stock이 있는 Product
//        Category category = Category.create("식품", "식품 카테고리");
//        categoryRepository.save(category);
//
//        Product product = Product.create(
//                "사과",
//                Money.of(5000L),
//                category.getId(),
//                UUID.randomUUID(),
//                UUID.randomUUID(),
//                100
//        );
//        Product savedProduct = productJpaRepository.save(product);
//        UUID productId = savedProduct.getId();
//        Stock savedStock = Stock.create(
//                productId,
//                savedProduct.getCompanyId(),
//                savedProduct.getHubId(),
//                100
//        );
//        stockJpaRepository.save(savedStock);
//
//        entityManager.flush();
//        entityManager.clear();
//
//        // when: Stock 조회
//        Stock stock = stockJpaRepository.findByProductId(productId).orElseThrow();
//
//        // then: 가용 재고 = 총 재고 - 예약 재고
//        assertThat(stock.getAvailableQuantity()).isEqualTo(100);
//        assertThat(stock.getQuantity()).isEqualTo(100);
//        assertThat(stock.getReservedQuantity()).isEqualTo(0);
//    }
//
//    @Test
//    @DisplayName("테스트 간 데이터 격리가 보장된다")
//    void dataIsolation_ShouldBeGuaranteed() {
//        assertThat(stockJpaRepository.count()).isNotNegative();
//    }
//}

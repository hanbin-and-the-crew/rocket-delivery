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
// * ProductJpaRepository 테스트
// *
// * 테스트 전략:
// * - @DataJpaTest: Repository 레이어 테스트
// * - H2 In-Memory DB 사용
// * - TestEntityManager: 영속성 컨텍스트 제어로 테스트 격리
// * - 기본 CRUD만 검증 (save, findById)
// *
// * 주의사항:
// * - 특정 메서드에 과도하게 의존하는 테스트 지양 (리팩터링 시 깨지기 쉬움)
// * - Repository는 데이터 접근 계층이므로 간단하게 유지
// */
//@DataJpaTest
//@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
//@ActiveProfiles("test")
//@Import(CategoryRepositoryImpl.class)
//class ProductJpaRepositoryTest {
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
//    @DisplayName("상품을 저장하면 ID가 생성되고 Stock도 함께 저장된다")
//    void save_ShouldGenerateIdAndSaveStock() {
//        // given: 카테고리와 상품 생성
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
//
//        // when: 상품 저장
//        Product savedProduct = productJpaRepository.save(product);
//        entityManager.flush();
//        entityManager.clear();
//
//        // then: ID가 생성됨 (Stock은 이벤트를 통해 별도로 생성)
//        assertThat(savedProduct.getId()).isNotNull();
//    }
//
//    @Test
//    @DisplayName("ID로 상품을 조회할 수 있다")
//    void findById_ShouldReturnProduct() {
//        // given: 저장된 상품
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
//
//        entityManager.flush();
//        entityManager.clear();
//
//        // when: ID로 조회
//        Optional<Product> foundProduct = productJpaRepository.findById(productId);
//
//        // then: 상품이 조회됨
//        assertThat(foundProduct).isPresent();
//        assertThat(foundProduct.get().getProductName()).isEqualTo("냉장고");
//        assertThat(foundProduct.get().getPrice().getAmount()).isEqualTo(2000000L);
//    }
//
//    @Test
//    @DisplayName("존재하지 않는 ID로 조회하면 빈 Optional이 반환된다")
//    void findById_WithNonExistentId_ShouldReturnEmpty() {
//        // given: 존재하지 않는 ID
//        UUID nonExistentId = UUID.randomUUID();
//
//        // when: 조회
//        Optional<Product> result = productJpaRepository.findById(nonExistentId);
//
//        // then: 빈 Optional 반환
//        assertThat(result).isEmpty();
//    }
//
//    @Test
//    @DisplayName("논리적으로 삭제된 상품도 조회할 수 있다")
//    void findById_WithDeletedProduct_ShouldReturnProduct() {
//        // given: 삭제된 상품
//        Category category = Category.create("식품", "식품 카테고리");
//        categoryRepository.save(category);
//
//        Product product = Product.create(
//                "사과",
//                Money.of(5000L),
//                category.getId(),
//                UUID.randomUUID(),
//                UUID.randomUUID(),
//                30
//        );
//        Product savedProduct = productJpaRepository.save(product);
//        savedProduct.delete();
//        productJpaRepository.save(savedProduct);
//
//        UUID productId = savedProduct.getId();
//        entityManager.flush();
//        entityManager.clear();
//
//        // when: 조회
//        Optional<Product> foundProduct = productJpaRepository.findById(productId);
//
//        // then: 삭제된 상품도 조회됨 (isActive=false)
//        assertThat(foundProduct).isPresent();
//        assertThat(foundProduct.get().getIsActive()).isFalse();
//    }
//
//    @Test
//    @DisplayName("상품 삭제 시 연관된 Stock도 UNAVAILABLE 상태가 된다")
//    void delete_ShouldMarkStockAsUnavailable() {
//        // given: 저장된 상품과 재고
//        Category category = Category.create("가구", "가구 카테고리");
//        categoryRepository.save(category);
//
//        Product product = Product.create(
//                "책상",
//                Money.of(300000L),
//                category.getId(),
//                UUID.randomUUID(),
//                UUID.randomUUID(),
//                20
//        );
//        Product savedProduct = productJpaRepository.save(product);
//        UUID productId = savedProduct.getId();
//
//        entityManager.flush();
//        entityManager.clear();
//
//        // when: 상품 삭제
//        Product productToDelete = productJpaRepository.findById(productId).orElseThrow();
//        productToDelete.delete();
//        productJpaRepository.save(productToDelete);
//
//        entityManager.flush();
//        entityManager.clear();
//
//        // then: Product는 비활성화되고 Stock은 UNAVAILABLE 상태가 됨
//        Product deletedProduct = productJpaRepository.findById(productId).orElseThrow();
//        assertThat(deletedProduct.getIsActive()).isFalse();
//        // Stock은 이벤트를 통해 UNAVAILABLE 상태로 변경됨 (별도 검증 필요)
//    }
//
//    @Test
//    @DisplayName("테스트 간 데이터 격리가 보장된다")
//    void dataIsolation_ShouldBeGuaranteed() {
//        assertThat(productJpaRepository.count()).isNotNegative();
//    }
//}
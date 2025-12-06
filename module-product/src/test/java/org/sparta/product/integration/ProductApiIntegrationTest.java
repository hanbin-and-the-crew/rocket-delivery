package org.sparta.product.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.product.domain.entity.Category;
import org.sparta.product.domain.entity.Product;
import org.sparta.product.domain.entity.Stock;
import org.sparta.product.domain.enums.StockStatus;
import org.sparta.product.domain.repository.CategoryRepository;
import org.sparta.product.domain.repository.ProductRepository;
import org.sparta.product.domain.repository.StockRepository;
import org.sparta.product.domain.vo.Money;
import org.sparta.product.infrastructure.jpa.ProductJpaRepository;
import org.sparta.product.infrastructure.jpa.StockJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Product API 통합 테스트
 * - Controller + Service + Repository + DB 전체 플로우 검증
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProductApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // 도메인 레포 (포트)
    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockRepository stockRepository;

    // 인프라 JPA 레포 (테스트에서만 편하게 사용)
    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private StockJpaRepository stockJpaRepository;

    @Test
    @DisplayName("상품 생성 시 Product/Stock이 함께 생성되고, 조회 API로 상세정보를 확인할 수 있다")
    void create_and_get_product_success() throws Exception {
        // given - 데이터 초기화
        categoryRepository.deleteAll();     // 지원됨
        productRepository.deleteAll();      // 지원됨
        stockJpaRepository.deleteAll();     // StockRepository에는 deleteAll이 없으므로 JPA 레포 사용

        // 카테고리 생성 (createProduct 에서 존재 여부를 검증함)
        Category category = Category.create("전자제품", "전자기기 카테고리");
        categoryRepository.save(category);

        UUID categoryId = category.getId();
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        int initialStock = 50;

        // 요청 JSON 구성 (presentation DTO에 의존하지 않고 직접 작성)
        ObjectNode json = objectMapper.createObjectNode();
        json.put("name", "통합테스트-노트북");
        json.put("price", 1_000_000L);
        json.put("categoryId", categoryId.toString());
        json.put("companyId", companyId.toString());
        json.put("hubId", hubId.toString());
        json.put("stock", initialStock);

        // when - 상품 생성 API 호출
        mockMvc.perform(
                        post("/api/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(json))
                )
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("통합테스트-노트북")));

        // then - DB에서 Product/Stock 직접 검증
        List<Product> products = productJpaRepository.findAll(); // 도메인 레포에는 findAll이 없어서 JPA 레포 사용
        Product savedProduct = products.stream()
                .filter(p -> "통합테스트-노트북".equals(p.getProductName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("생성된 상품을 찾을 수 없습니다."));

        Stock stock = stockRepository.findByProductId(savedProduct.getId())
                .orElseThrow(() -> new AssertionError("생성된 재고를 찾을 수 없습니다."));

        assertThat(stock.getQuantity()).isEqualTo(initialStock);
        assertThat(stock.getReservedQuantity()).isZero();

        // 상세 조회 API도 이 정보를 그대로 돌려주는지 확인
        mockMvc.perform(
                        get("/api/products/{productId}", savedProduct.getId())
                )
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("통합테스트-노트북")))
                .andExpect(content().string(containsString(String.valueOf(initialStock))));
    }

    @Test
    @DisplayName("상품 삭제 시 Product는 비활성화되고, Stock도 UNAVAILABLE 상태가 된다")
    void delete_product_disables_stock() throws Exception {
        // given - 초기화
        categoryRepository.deleteAll();
        productRepository.deleteAll();
        stockJpaRepository.deleteAll();

        Category category = Category.create("삭제-카테고리", "삭제 테스트용");
        categoryRepository.save(category);

        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        Product product = Product.create(
                "삭제-테스트-상품",
                Money.of(10_000L),
                category.getId(),
                companyId,
                hubId,
                100
        );
        productRepository.save(product);

        Stock stock = Stock.create(
                product.getId(),
                companyId,
                hubId,
                100
        );
        stockRepository.save(stock);

        // when - 삭제 API 호출
        mockMvc.perform(
                        delete("/api/products/{productId}", product.getId())
                )
                .andExpect(status().isOk());

        // then - Product 논리삭제, Stock UNAVAILABLE
        Product deletedProduct = productRepository.findById(product.getId())
                .orElseThrow(() -> new AssertionError("삭제 후 상품을 찾을 수 없습니다."));

        assertThat(deletedProduct.getIsActive()).isFalse();

        Stock updatedStock = stockRepository.findByProductId(product.getId())
                .orElseThrow(() -> new AssertionError("삭제 후 재고를 찾을 수 없습니다."));

        assertThat(updatedStock.getStatus()).isEqualTo(StockStatus.UNAVAILABLE);
    }
}

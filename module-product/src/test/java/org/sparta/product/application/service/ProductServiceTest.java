package org.sparta.product.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sparta.common.error.BusinessException;
import org.sparta.common.event.EventPublisher;
import org.sparta.product.domain.entity.Product;
import org.sparta.product.domain.entity.Stock;
import org.sparta.product.domain.error.ProductErrorType;
import org.sparta.product.domain.repository.CategoryRepository;
import org.sparta.product.domain.repository.ProductRepository;
import org.sparta.product.domain.repository.StockRepository;
import org.sparta.product.presentation.ProductRequest;
import org.sparta.product.presentation.ProductResponse;
import org.sparta.product.support.fixtures.ProductFixture;
import org.sparta.product.support.fixtures.StockFixture;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.verify;
import org.mockito.ArgumentCaptor;

/**
 * ProductService 테스트
 *
 * 요구사항:
 * 1. 상품과 재고는 함께 생성이 되어야한다. 즉 재고는 상품을 통해 생성되어야한다.
 *
 * 테스트 전략:
 * - Mock: ProductRepository, CategoryRepository (외부 의존성 격리)
 * - 실제 객체: ProductService (테스트 대상)
 * - Fixture: ProductFixture 사용으로 테스트 데이터 생성
 *
 * 핵심 검증 사항:
 * 1. 상품 생성 시 Stock도 함께 생성되는지 (Cascade)
 * 2. 상품 조회 시 올바르게 반환되는지, 없을 경우 예외가 발생하는지
 *
 * 참고: 재고 차감/복원 테스트는 StockServiceTest로 분리됨
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private ProductService productService;

    @Test
    @DisplayName("도메인 요구사항에 맞는 상품 정보를 입력하면 상품이 생성된다")
    void createProduct_WithValidInput_ShouldSucceed() {
        // given: 유효한 상품 생성 요청
        String productName = "테스트 상품";
        Long price = 10000L;
        UUID categoryId = UUID.randomUUID();
        Integer initialQuantity = 10;

        ProductRequest.Create request = new ProductRequest.Create(
                productName,
                price,
                categoryId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                initialQuantity
        );

        given(categoryRepository.existsById(categoryId))
                .willReturn(true);
        given(productRepository.save(any(Product.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        willDoNothing().given(eventPublisher).publishLocal(any());

        // when: 상품 생성
        ProductResponse.Create response = productService.createProduct(request);

        // then: 상품과 재고가 함께 생성됨
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());

        Product savedProduct = productCaptor.getValue();
        assertThat(savedProduct).isNotNull();
        assertThat(savedProduct.getProductName()).isEqualTo(productName);
        assertThat(savedProduct.getPrice().getAmount()).isEqualTo(price);
        // Stock은 이벤트를 통해 별도로 생성됨

        // 응답 검증
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo(productName);
        assertThat(response.price()).isEqualTo(price);
    }

    @Test
    @DisplayName("유효한 ID로 상품을 조회하면 반환된다")
    void getProduct_WithValidId_ReturnsProduct() {
        // given: 존재하는 상품
        Product product = ProductFixture.defaultProduct();
        UUID productId = product.getId();
        Stock stock = StockFixture.withProductIdAndQuantity(
                productId,
                product.getCompanyId(),
                product.getHubId(),
                100
        );

        given(productRepository.findById(productId))
                .willReturn(Optional.of(product));
        given(stockRepository.findByProductId(productId))
                .willReturn(Optional.of(stock));

        // when: 상품 조회
        ProductResponse.Detail response = productService.getProduct(productId);

        // then: 상품 정보가 반환됨
        assertThat(response).isNotNull();
        assertThat(response.productId()).isEqualTo(productId);
        assertThat(response.name()).isEqualTo(product.getProductName());
        assertThat(response.price()).isEqualTo(product.getPrice().getAmount());
    }

    @Test
    @DisplayName("존재하지 않는 상품을 조회하면 PRODUCT_NOT_FOUND 예외가 발생한다")
    void getProduct_WithInvalidId_ShouldThrowException() {
        // given: 존재하지 않는 상품 ID
        UUID invalidProductId = UUID.randomUUID();

        given(productRepository.findById(invalidProductId))
                .willReturn(Optional.empty());

        // when & then: 예외 발생
        assertThatThrownBy(() -> productService.getProduct(invalidProductId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorType", ProductErrorType.PRODUCT_NOT_FOUND);
    }
}

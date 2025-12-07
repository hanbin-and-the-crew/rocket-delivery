package org.sparta.product.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sparta.common.error.BusinessException;
import org.sparta.product.application.dto.ProductCreateCommand;
import org.sparta.product.application.dto.ProductDetailInfo;
import org.sparta.product.application.dto.ProductUpdateCommand;
import org.sparta.product.domain.entity.Product;
import org.sparta.product.domain.entity.Stock;
import org.sparta.product.domain.enums.StockStatus;
import org.sparta.product.domain.error.ProductErrorType;
import org.sparta.product.domain.repository.CategoryRepository;
import org.sparta.product.domain.repository.ProductRepository;
import org.sparta.product.domain.repository.StockRepository;
import org.sparta.product.domain.vo.Money;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * ProductService 애플리케이션 계층 단위 테스트
 *
 * - 도메인 규칙(Product, Stock 내부 로직)은 도메인 테스트에서 검증한다고 가정하고
 * - 여기서는 "서비스가 레포지토리/도메인을 어떤 순서와 정책으로 호출하는지"를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private ProductService productService;

    /**
     * 테스트에서 엔티티의 private 필드(id 등)를 세팅하기 위한 헬퍼
     */
    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ======================================================================
    // createProduct
    // ======================================================================

    @Test
    @DisplayName("카테고리가 존재하면 상품과 재고를 생성하고 상품 ID를 반환한다")
    void createProduct_success() {
        // given
        UUID categoryId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        UUID generatedProductId = UUID.randomUUID();

        String productName = "테스트 상품";
        long price = 10_000L;
        int initialQuantity = 50;

        ProductCreateCommand command = new ProductCreateCommand(
                productName,
                price,
                categoryId,
                companyId,
                hubId,
                initialQuantity
        );

        // 카테고리 존재
        given(categoryRepository.existsById(categoryId)).willReturn(true);

        // save(Product) 호출 시 id를 세팅해주는 스텁
        given(productRepository.save(any(Product.class)))
                .willAnswer(invocation -> {
                    Product p = invocation.getArgument(0);
                    setField(p, "id", generatedProductId);
                    return p;
                });

        // save(Stock)는 그대로 반환만
        given(stockRepository.save(any(Stock.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        ArgumentCaptor<Stock> stockCaptor = ArgumentCaptor.forClass(Stock.class);

        // when
        UUID resultId = productService.createProduct(command);

        // then
        assertThat(resultId).isEqualTo(generatedProductId);

        verify(categoryRepository).existsById(categoryId);
        verify(productRepository).save(productCaptor.capture());
        verify(stockRepository).save(stockCaptor.capture());

        Product savedProduct = productCaptor.getValue();
        Stock savedStock = stockCaptor.getValue();

        // Product 검증
        assertThat(savedProduct.getProductName()).isEqualTo(productName);
        assertThat(savedProduct.getPrice().getAmount()).isEqualTo(price);
        assertThat(savedProduct.getCategoryId()).isEqualTo(categoryId);
        assertThat(savedProduct.getCompanyId()).isEqualTo(companyId);
        assertThat(savedProduct.getHubId()).isEqualTo(hubId);
        assertThat(savedProduct.getId()).isEqualTo(generatedProductId);

        // Stock 검증
        assertThat(savedStock.getProductId()).isEqualTo(generatedProductId);
        assertThat(savedStock.getCompanyId()).isEqualTo(companyId);
        assertThat(savedStock.getHubId()).isEqualTo(hubId);
        assertThat(savedStock.getQuantity()).isEqualTo(initialQuantity);
        assertThat(savedStock.getReservedQuantity()).isZero();
        assertThat(savedStock.getStatus()).isEqualTo(StockStatus.IN_STOCK);
    }

    @Test
    @DisplayName("카테고리가 존재하지 않으면 상품 생성 시 CATEGORY_ID_REQUIRED 예외가 발생한다")
    void createProduct_categoryNotFound_shouldThrowException() {
        // given
        UUID categoryId = UUID.randomUUID();
        ProductCreateCommand command = new ProductCreateCommand(
                "상품",
                10_000L,
                categoryId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                10
        );

        given(categoryRepository.existsById(categoryId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> productService.createProduct(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorType", ProductErrorType.CATEGORY_ID_REQUIRED);

        verify(categoryRepository).existsById(categoryId);
        verifyNoInteractions(productRepository);
        verifyNoInteractions(stockRepository);
    }

    // ======================================================================
    // getProduct
    // ======================================================================

    @Test
    @DisplayName("상품 ID로 상품 상세 조회에 성공하면 ProductDetailInfo를 반환한다")
    void getProduct_success() {
        // given
        UUID productId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        String productName = "상품 A";
        long price = 5_000L;
        int initialQuantity = 30;

        Product product = Product.create(
                productName,
                Money.of(price),
                categoryId,
                companyId,
                hubId,
                initialQuantity
        );
        setField(product, "id", productId);

        Stock stock = Stock.create(
                productId,
                companyId,
                hubId,
                initialQuantity
        );

        given(productRepository.findById(productId)).willReturn(Optional.of(product));
        given(stockRepository.findByProductId(productId)).willReturn(Optional.of(stock));

        // when
        ProductDetailInfo detail = productService.getProduct(productId);

        // then
        assertThat(detail.productId()).isEqualTo(productId);
        assertThat(detail.productName()).isEqualTo(productName);
        assertThat(detail.price()).isEqualTo(price);
        assertThat(detail.categoryId()).isEqualTo(categoryId);
        assertThat(detail.companyId()).isEqualTo(companyId);
        assertThat(detail.hubId()).isEqualTo(hubId);
        assertThat(detail.quantity()).isEqualTo(initialQuantity);
        assertThat(detail.reservedQuantity()).isZero();
        assertThat(detail.isActive()).isTrue();

        verify(productRepository).findById(productId);
        verify(stockRepository).findByProductId(productId);
    }

    @Test
    @DisplayName("상품 상세 조회 시 상품이 존재하지 않으면 PRODUCT_NOT_FOUND 예외가 발생한다")
    void getProduct_productNotFound_shouldThrowException() {
        // given
        UUID productId = UUID.randomUUID();
        given(productRepository.findById(productId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.getProduct(productId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorType", ProductErrorType.PRODUCT_NOT_FOUND);

        verify(productRepository).findById(productId);
        verifyNoInteractions(stockRepository);
    }

    @Test
    @DisplayName("상품 상세 조회 시 재고가 존재하지 않으면 STOCK_NOT_FOUND 예외가 발생한다")
    void getProduct_stockNotFound_shouldThrowException() {
        // given
        UUID productId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        Product product = Product.create(
                "상품 A",
                Money.of(5_000L),
                categoryId,
                companyId,
                hubId,
                10
        );
        setField(product, "id", productId);

        given(productRepository.findById(productId)).willReturn(Optional.of(product));
        given(stockRepository.findByProductId(productId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.getProduct(productId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorType", ProductErrorType.STOCK_NOT_FOUND);

        verify(productRepository).findById(productId);
        verify(stockRepository).findByProductId(productId);
    }

    // ======================================================================
    // updateProduct
    // ======================================================================

    @Test
    @DisplayName("상품 수정 시 이름과 가격이 변경된다")
    void updateProduct_success() {
        // given
        UUID productId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        Product product = Product.create(
                "기존 상품명",
                Money.of(5_000L),
                categoryId,
                companyId,
                hubId,
                10
        );
        setField(product, "id", productId);

        given(productRepository.findById(productId)).willReturn(Optional.of(product));
        given(productRepository.save(any(Product.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        String newName = "수정된 상품명";
        long newPrice = 7_000L;

        ProductUpdateCommand command = new ProductUpdateCommand(
                newName,
                newPrice
        );

        // when
        productService.updateProduct(productId, command);

        // then
        assertThat(product.getProductName()).isEqualTo(newName);
        assertThat(product.getPrice().getAmount()).isEqualTo(newPrice);

        verify(productRepository).findById(productId);
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("상품 수정 시 상품이 존재하지 않으면 PRODUCT_NOT_FOUND 예외가 발생한다")
    void updateProduct_productNotFound_shouldThrowException() {
        // given
        UUID productId = UUID.randomUUID();
        given(productRepository.findById(productId)).willReturn(Optional.empty());

        ProductUpdateCommand command = new ProductUpdateCommand(
                "새 이름",
                10_000L
        );

        // when & then
        assertThatThrownBy(() -> productService.updateProduct(productId, command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorType", ProductErrorType.PRODUCT_NOT_FOUND);

        verify(productRepository).findById(productId);
        verify(productRepository, never()).save(any(Product.class));
    }

    // ======================================================================
    // deleteProduct
    // ======================================================================

    @Test
    @DisplayName("상품 삭제 시 isActive=false로 변경되고 재고는 UNAVAILABLE 상태가 된다")
    void deleteProduct_success() {
        // given
        UUID productId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        Product product = Product.create(
                "삭제할 상품",
                Money.of(10_000L),
                categoryId,
                companyId,
                hubId,
                10
        );
        setField(product, "id", productId);

        Stock stock = Stock.create(
                productId,
                companyId,
                hubId,
                10
        );

        given(productRepository.findById(productId)).willReturn(Optional.of(product));
        given(productRepository.save(any(Product.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        given(stockRepository.findByProductId(productId)).willReturn(Optional.of(stock));
        given(stockRepository.save(any(Stock.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        productService.deleteProduct(productId);

        // then
        // 상품 논리 삭제
        assertThat(product.getIsActive()).isFalse();

        // 재고 판매 불가 상태
        assertThat(stock.getStatus()).isEqualTo(StockStatus.UNAVAILABLE);

        verify(productRepository).findById(productId);
        verify(productRepository).save(product);
        verify(stockRepository).findByProductId(productId);
        verify(stockRepository).save(stock);
    }

    @Test
    @DisplayName("상품 삭제 시 상품이 존재하지 않으면 PRODUCT_NOT_FOUND 예외가 발생한다")
    void deleteProduct_productNotFound_shouldThrowException() {
        // given
        UUID productId = UUID.randomUUID();
        given(productRepository.findById(productId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.deleteProduct(productId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorType", ProductErrorType.PRODUCT_NOT_FOUND);

        verify(productRepository).findById(productId);
        verifyNoInteractions(stockRepository);
    }
}

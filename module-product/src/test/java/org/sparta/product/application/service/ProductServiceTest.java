package org.sparta.product.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sparta.common.error.BusinessException;
import org.sparta.product.application.dto.ProductCreateCommand;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock ProductRepository productRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock StockRepository stockRepository;

    ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, categoryRepository, stockRepository);
    }

    @Test
    @DisplayName("createProduct: category 미존재 시 CATEGORY_ID_REQUIRED")
    void createProduct_categoryMissing() {
        UUID categoryId = UUID.randomUUID();
        when(categoryRepository.existsById(categoryId)).thenReturn(false);

        ProductCreateCommand cmd = new ProductCreateCommand(
                "상품",
                1000L,
                categoryId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                10
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> productService.createProduct(cmd));
        assertEquals(ProductErrorType.CATEGORY_ID_REQUIRED, ex.getErrorType());

        verify(productRepository, never()).save(any());
        verify(stockRepository, never()).save(any());
    }

    @Test
    @DisplayName("createProduct: Product 저장 후 Stock 동기 생성 및 저장")
    void createProduct_success_createsStock() {
        UUID categoryId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        when(categoryRepository.existsById(categoryId)).thenReturn(true);

        UUID savedProductId = UUID.randomUUID();
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            setId(p, savedProductId);
            return p;
        });
        when(stockRepository.save(any(Stock.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductCreateCommand cmd = new ProductCreateCommand(
                "상품",
                1000L,
                categoryId,
                companyId,
                hubId,
                7
        );

        UUID result = productService.createProduct(cmd);
        assertEquals(savedProductId, result);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        Product saved = productCaptor.getValue();
        assertEquals("상품", saved.getProductName());
        assertEquals(1000L, saved.getPrice().getAmount());
        assertEquals(categoryId, saved.getCategoryId());
        assertEquals(companyId, saved.getCompanyId());
        assertEquals(hubId, saved.getHubId());
        assertTrue(saved.getIsActive());

        ArgumentCaptor<Stock> stockCaptor = ArgumentCaptor.forClass(Stock.class);
        verify(stockRepository).save(stockCaptor.capture());
        Stock stock = stockCaptor.getValue();
        assertEquals(savedProductId, stock.getProductId());
        assertEquals(companyId, stock.getCompanyId());
        assertEquals(hubId, stock.getHubId());
        assertEquals(7, stock.getQuantity());
        assertEquals(0, stock.getReservedQuantity());
        assertEquals(StockStatus.IN_STOCK, stock.getStatus());
    }

    @Test
    @DisplayName("getProduct: product 없으면 PRODUCT_NOT_FOUND")
    void getProduct_productNotFound() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> productService.getProduct(productId));
        assertEquals(ProductErrorType.PRODUCT_NOT_FOUND, ex.getErrorType());
    }

    @Test
    @DisplayName("getProduct: stock 없으면 STOCK_NOT_FOUND")
    void getProduct_stockNotFound() {
        UUID productId = UUID.randomUUID();
        Product product = Product.create("상품", Money.of(1000L), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 0);
        setId(product, productId);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(stockRepository.findByProductId(productId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> productService.getProduct(productId));
        assertEquals(ProductErrorType.STOCK_NOT_FOUND, ex.getErrorType());
    }

    @Test
    @DisplayName("updateProduct: 상품명/가격 선택적 수정 후 save 호출")
    void updateProduct_updatesFields() {
        UUID productId = UUID.randomUUID();
        Product product = Product.create("기존", Money.of(1000L), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 0);
        setId(product, productId);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductUpdateCommand cmd1 = new ProductUpdateCommand("변경", null);
        productService.updateProduct(productId, cmd1);

        assertEquals("변경", product.getProductName());
        assertEquals(1000L, product.getPrice().getAmount());
        verify(productRepository).save(product);

        ProductUpdateCommand cmd2 = new ProductUpdateCommand(null, 2000L);
        productService.updateProduct(productId, cmd2);

        assertEquals("변경", product.getProductName());
        assertEquals(2000L, product.getPrice().getAmount());
        verify(productRepository, times(2)).save(product);
    }

    @Test
    @DisplayName("deleteProduct: Product delete + Stock markAsUnavailable + save")
    void deleteProduct_marksInactive_and_stockUnavailable() {
        UUID productId = UUID.randomUUID();
        Product product = Product.create("상품", Money.of(1000L), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 0);
        setId(product, productId);

        Stock stock = Stock.create(productId, UUID.randomUUID(), UUID.randomUUID(), 10);
        setId(stock, UUID.randomUUID());

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(stockRepository.findByProductId(productId)).thenReturn(Optional.of(stock));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stockRepository.save(any(Stock.class))).thenAnswer(inv -> inv.getArgument(0));

        productService.deleteProduct(productId);

        assertFalse(product.getIsActive());
        assertEquals(StockStatus.UNAVAILABLE, stock.getStatus());

        verify(productRepository).save(product);
        verify(stockRepository).save(stock);
    }

    private static void setId(Object entity, UUID id) {
        try {
            Field f = entity.getClass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

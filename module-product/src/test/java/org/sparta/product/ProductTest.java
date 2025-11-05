package org.sparta.product;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductTest {

    @Test
    @DisplayName("상품 생성 테스트")
    void createProduct() {
        // Given & When
        Product product = new Product(1L, "테스트 상품", 10000, 100);

        // Then
        assertEquals(1L, product.getId());
        assertEquals("테스트 상품", product.getName());
        assertEquals(10000, product.getPrice());
        assertEquals(100, product.getStock());
    }

    @Test
    @DisplayName("재고 차감 성공 테스트")
    void decreaseStock_Success() {
        // Given
        Product product = new Product(1L, "테스트 상품", 10000, 100);

        // When
        product.decreaseStock(10);

        // Then
        assertEquals(80, product.getStock());
    }

    @Test
    @DisplayName("재고 부족 시 예외 발생 테스트")
    void decreaseStock_InsufficientStock() {
        // Given
        Product product = new Product(1L, "테스트 상품", 10000,1 );

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> product.decreaseStock(10)
        );
        assertEquals("재고가 부족합니다.", exception.getMessage());
    }

    @Test
    @DisplayName("재고 증가 테스트")
    void increaseStock() {
        // Given
        Product product = new Product(1L, "테스트 상품", 10000, 100);

        // When
        product.increaseStock(50);

        // Then
        assertEquals(150, product.getStock());
    }

    @Test
    @DisplayName("재고 존재 여부 확인 테스트")
    void isAvailable() {
        // Given
        Product productWithStock = new Product(1L, "재고 있는 상품", 10000, 10);
        Product productWithoutStock = new Product(2L, "재고 없는 상품", 10000, 0);

        // When & Then
        assertTrue(productWithStock.isAvailable());
        assertFalse(productWithoutStock.isAvailable());
    }
}
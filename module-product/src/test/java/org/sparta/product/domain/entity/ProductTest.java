package org.sparta.product.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.common.error.BusinessException;
import org.sparta.product.domain.error.ProductErrorType;
import org.sparta.product.domain.vo.Money;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProductTest {

    @Test
    @DisplayName("Product.create: 필수값 누락 시 BusinessException(ProductErrorType) 발생")
    void create_requiredValidation() {
        UUID categoryId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        BusinessException ex1 = assertThrows(BusinessException.class,
                () -> Product.create(" ", Money.of(1000L), categoryId, companyId, hubId, 0));
        assertEquals(ProductErrorType.PRODUCT_NAME_REQUIRED, ex1.getErrorType());

        BusinessException ex2 = assertThrows(BusinessException.class,
                () -> Product.create("상품", null, categoryId, companyId, hubId, 0));
        assertEquals(ProductErrorType.PRICE_REQUIRED, ex2.getErrorType());

        BusinessException ex3 = assertThrows(BusinessException.class,
                () -> Product.create("상품", Money.of(1000L), null, companyId, hubId, 0));
        assertEquals(ProductErrorType.CATEGORY_ID_REQUIRED, ex3.getErrorType());

        BusinessException ex4 = assertThrows(BusinessException.class,
                () -> Product.create("상품", Money.of(1000L), categoryId, null, hubId, 0));
        assertEquals(ProductErrorType.COMPANY_ID_REQUIRED, ex4.getErrorType());

        BusinessException ex5 = assertThrows(BusinessException.class,
                () -> Product.create("상품", Money.of(1000L), categoryId, companyId, null, 0));
        assertEquals(ProductErrorType.HUB_ID_REQUIRED, ex5.getErrorType());

        BusinessException ex6 = assertThrows(BusinessException.class,
                () -> Product.create("상품", Money.of(1000L), categoryId, companyId, hubId, -1));
        assertEquals(ProductErrorType.INITIAL_QUANTITY_INVALID, ex6.getErrorType());
    }

    @Test
    @DisplayName("Product.update: null/blank는 유지, 유효 값은 변경")
    void update_partialUpdate() {
        Product product = Product.create(
                "기존명",
                Money.of(1000L),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                10
        );

        product.update(" ", null);
        assertEquals("기존명", product.getProductName());
        assertEquals(1000L, product.getPrice().getAmount());

        product.update("변경명", Money.of(2000L));
        assertEquals("변경명", product.getProductName());
        assertEquals(2000L, product.getPrice().getAmount());
    }

    @Test
    @DisplayName("Product.delete: isActive=false")
    void delete_marksInactive() {
        Product product = Product.create(
                "상품",
                Money.of(1000L),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                0
        );

        assertTrue(product.getIsActive());
        product.delete();
        assertFalse(product.getIsActive());
    }
}

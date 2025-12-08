package org.sparta.product.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.sparta.common.error.BusinessException;
import org.sparta.product.domain.vo.Money;
import org.sparta.product.support.fixtures.ProductFixture;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Product 도메인 단위 테스트
 * - Product는 Stock과 독립된 애그리거트
 * - 생성, 수정, 삭제 등 도메인 로직만 검증
 * - Stock 생성은 이벤트를 통해 처리되므로 여기서는 다루지 않는다.
 */
@DisplayName("Product 도메인 테스트")
class ProductTest {

    private static final UUID TEST_CATEGORY_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID TEST_COMPANY_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID TEST_HUB_ID     = UUID.fromString("30000000-0000-0000-0000-000000000003");

    @Test
    @DisplayName("유효한 입력으로 상품을 생성하면 필드가 올바르게 세팅된다")
    void create_WithValidInput_ShouldSucceed() {
        // given
        String productName = "테스트 상품";
        Money price = Money.of(10_000L);
        Integer initialQuantity = 100;

        // when
        Product product = Product.create(
                productName,
                price,
                TEST_CATEGORY_ID,
                TEST_COMPANY_ID,
                TEST_HUB_ID,
                initialQuantity
        );

        // then
        assertThat(product).isNotNull();
        assertThat(product.getProductName()).isEqualTo(productName);
        assertThat(product.getPrice()).isEqualTo(price);
        assertThat(product.getCategoryId()).isEqualTo(TEST_CATEGORY_ID);
        assertThat(product.getCompanyId()).isEqualTo(TEST_COMPANY_ID);
        assertThat(product.getHubId()).isEqualTo(TEST_HUB_ID);
        // isActive 필드는 Boolean isActive 이므로 Lombok이 getIsActive()를 생성
        assertThat(product.getIsActive()).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("상품명이 null이거나 공백이면 예외가 발생한다")
    void create_WithInvalidProductName_ShouldThrowException(String invalidName) {
        // when & then
        assertThatThrownBy(() -> Product.create(
                invalidName,
                Money.of(10_000L),
                TEST_CATEGORY_ID,
                TEST_COMPANY_ID,
                TEST_HUB_ID,
                100
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("상품명은 필수입니다");
    }

    @Test
    @DisplayName("가격이 null이면 예외가 발생한다")
    void create_WithNullPrice_ShouldThrowException() {
        // when & then
        assertThatThrownBy(() -> Product.create(
                "테스트 상품",
                null,
                TEST_CATEGORY_ID,
                TEST_COMPANY_ID,
                TEST_HUB_ID,
                100
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("가격은 필수입니다");
    }

    @Test
    @DisplayName("Money 값이 음수이면 예외가 발생한다")
    void money_WithNegativeAmount_ShouldThrowException() {
        // when & then
        assertThatThrownBy(() -> Money.of(-1000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("금액은 음수일 수 없습니다");
    }

    @Test
    @DisplayName("카테고리 ID가 null이면 예외가 발생한다")
    void create_WithNullCategoryId_ShouldThrowException() {
        assertThatThrownBy(() -> Product.create(
                "테스트 상품",
                Money.of(10_000L),
                null,
                TEST_COMPANY_ID,
                TEST_HUB_ID,
                100
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("카테고리 ID는 필수입니다");
    }

    @Test
    @DisplayName("업체 ID가 null이면 예외가 발생한다")
    void create_WithNullCompanyId_ShouldThrowException() {
        assertThatThrownBy(() -> Product.create(
                "테스트 상품",
                Money.of(10_000L),
                TEST_CATEGORY_ID,
                null,
                TEST_HUB_ID,
                100
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("업체 ID는 필수입니다");
    }

    @Test
    @DisplayName("허브 ID가 null이면 예외가 발생한다")
    void create_WithNullHubId_ShouldThrowException() {
        assertThatThrownBy(() -> Product.create(
                "테스트 상품",
                Money.of(10_000L),
                TEST_CATEGORY_ID,
                TEST_COMPANY_ID,
                null,
                100
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("허브 ID는 필수입니다");
    }

    @Test
    @DisplayName("초기 재고가 음수이면 예외가 발생한다")
    void create_WithNegativeInitialQuantity_ShouldThrowException() {
        assertThatThrownBy(() -> Product.create(
                "테스트 상품",
                Money.of(10_000L),
                TEST_CATEGORY_ID,
                TEST_COMPANY_ID,
                TEST_HUB_ID,
                -1
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("재고량은 0 이상이어야 합니다");
    }

    @Test
    @DisplayName("초기 재고가 null이면 예외가 발생한다")
    void create_WithNullInitialQuantity_ShouldThrowException() {
        assertThatThrownBy(() -> Product.create(
                "테스트 상품",
                Money.of(10_000L),
                TEST_CATEGORY_ID,
                TEST_COMPANY_ID,
                TEST_HUB_ID,
                null
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("재고량은 0 이상이어야 합니다");
    }

    @Test
    @DisplayName("초기 재고가 0이어도 상품 생성은 가능하다")
    void create_WithZeroInitialQuantity_ShouldSucceed() {
        Product product = Product.create(
                "테스트 상품",
                Money.of(10_000L),
                TEST_CATEGORY_ID,
                TEST_COMPANY_ID,
                TEST_HUB_ID,
                0
        );

        assertThat(product).isNotNull();
    }

    @Test
    @DisplayName("상품 정보를 수정하면 값이 변경된다")
    void update_WithValidValues_ShouldUpdateFields() {
        // given
        Product product = ProductFixture.defaultProduct();
        String newName = "수정된 상품명";
        Money newPrice = Money.of(20_000L);

        // when
        product.update(newName, newPrice);

        // then
        assertThat(product.getProductName()).isEqualTo(newName);
        assertThat(product.getPrice()).isEqualTo(newPrice);
    }

    @Test
    @DisplayName("update에 null을 전달하면 기존 값이 유지된다")
    void update_WithNullValues_ShouldKeepOriginal() {
        // given
        Product product = ProductFixture.defaultProduct();
        String originalName = product.getProductName();
        Money originalPrice = product.getPrice();

        // when
        product.update(null, null);

        // then
        assertThat(product.getProductName()).isEqualTo(originalName);
        assertThat(product.getPrice()).isEqualTo(originalPrice);
    }

    @Test
    @DisplayName("상품을 삭제하면 isActive가 false가 된다")
    void delete_ShouldSetInactive() {
        // given
        Product product = ProductFixture.defaultProduct();
        assertThat(product.getIsActive()).isTrue();

        // when
        product.delete();

        // then
        assertThat(product.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("이미 삭제된 상품을 다시 삭제해도 isActive는 그대로 false이다 (멱등성)")
    void delete_AlreadyDeleted_ShouldRemainInactive() {
        // given
        Product product = ProductFixture.defaultProduct();
        product.delete();
        assertThat(product.getIsActive()).isFalse();

        // when
        product.delete();

        // then
        assertThat(product.getIsActive()).isFalse();
    }
}

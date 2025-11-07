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
 * 생성 시점에 반드시 수행해야하는 점
 * - 상품 업체가 존재하는가.
 * - 상품 관리 허브 ID를 확인하여 존재하는가.
 * - 상품 생성 시 반드시 재고가 생성이 되는가
 * - 등록시 재고가 0 이상인가
 * - 상품 삭제 시 재고가 함께 삭제가 되는가?
 * - 상품 개당 금액이 0원 이상인가?
 */
class ProductTest {

    private static final UUID TEST_CATEGORY_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID TEST_COMPANY_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID TEST_HUB_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");

    @Test
    @DisplayName("유효한 입력으로 상품을 생성하면 성공한다")
    void create_WithValidInput_ShouldSucceed() {
        // given: 유효한 상품 정보
        String productName = "테스트 상품";
        Money price = Money.of(10000L);
        UUID categoryId = TEST_CATEGORY_ID;
        UUID companyId = TEST_COMPANY_ID;
        UUID hubId = TEST_HUB_ID;
        Integer initialQuantity = 100;

        // when: 상품 생성
        Product product = Product.create(
                productName,
                price,
                categoryId,
                companyId,
                hubId,
                initialQuantity
        );

        // then: 상품이 정상적으로 생성됨
        assertThat(product).isNotNull();
        assertThat(product.getProductName()).isEqualTo(productName);
        assertThat(product.getPrice()).isEqualTo(price);
        assertThat(product.getCategoryId()).isEqualTo(categoryId);
        assertThat(product.getCompanyId()).isEqualTo(companyId);
        assertThat(product.getHubId()).isEqualTo(hubId);
        assertThat(product.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("상품 생성 시 재고가 함께 생성된다")
    void create_WithValidInput_ShouldCreateStock() {
        // given: 유효한 상품 정보와 초기 재고량
        Integer initialQuantity = 100;

        // when: 상품 생성
        Product product = Product.create(
                "테스트 상품",
                Money.of(10000L),
                TEST_CATEGORY_ID,
                TEST_COMPANY_ID,
                TEST_HUB_ID,
                initialQuantity
        );

        // then: 재고가 함께 생성됨
        assertThat(product.getStock()).isNotNull();
        assertThat(product.getStock().getQuantity()).isEqualTo(initialQuantity);
        assertThat(product.getStock().getReservedQuantity()).isEqualTo(0);
        assertThat(product.getStock().getProduct()).isEqualTo(product);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("상품명이 null이거나 빈 문자열이면 예외가 발생한다")
    void create_WithInvalidProductName_ShouldThrowException(String invalidName) {
        // given: 유효하지 않은 상품명
        Money price = Money.of(10000L);

        // when & then: 예외 발생
        assertThatThrownBy(() -> Product.create(
                invalidName,
                price,
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
        // given: null 가격
        Money nullPrice = null;

        // when & then: 예외 발생
        assertThatThrownBy(() -> Product.create(
                "테스트 상품",
                nullPrice,
                TEST_CATEGORY_ID,
                TEST_COMPANY_ID,
                TEST_HUB_ID,
                100
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("가격은 필수입니다");
    }

    @Test
    @DisplayName("가격이 음수이면 Money 객체 생성 시 예외가 발생한다")
    void create_WithNegativePrice_ShouldThrowException() {
        // when & then: Money 객체 생성 시 예외 발생
        assertThatThrownBy(() -> Money.of(-1000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("금액은 음수일 수 없습니다");
    }

    @Test
    @DisplayName("카테고리 ID가 null이면 예외가 발생한다")
    void create_WithNullCategoryId_ShouldThrowException() {
        // given: null 카테고리 ID
        UUID nullCategoryId = null;

        // when & then: 예외 발생
        assertThatThrownBy(() -> Product.create(
                "테스트 상품",
                Money.of(10000L),
                nullCategoryId,
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
        // given: null 업체 ID
        UUID nullCompanyId = null;

        // when & then: 예외 발생
        assertThatThrownBy(() -> Product.create(
                "테스트 상품",
                Money.of(10000L),
                TEST_CATEGORY_ID,
                nullCompanyId,
                TEST_HUB_ID,
                100
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("업체 ID는 필수입니다");
    }

    @Test
    @DisplayName("허브 ID가 null이면 예외가 발생한다")
    void create_WithNullHubId_ShouldThrowException() {
        // given: null 허브 ID
        UUID nullHubId = null;

        // when & then: 예외 발생
        assertThatThrownBy(() -> Product.create(
                "테스트 상품",
                Money.of(10000L),
                TEST_CATEGORY_ID,
                TEST_COMPANY_ID,
                nullHubId,
                100
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("허브 ID는 필수입니다");
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -10, -100})
    @DisplayName("재고량이 음수이면 예외가 발생한다")
    void create_WithNegativeQuantity_ShouldThrowException(Integer negativeQuantity) {
        // given: 음수 재고량
        Money price = Money.of(10000L);

        // when & then: 예외 발생
        assertThatThrownBy(() -> Product.create(
                "테스트 상품",
                price,
                TEST_CATEGORY_ID,
                TEST_COMPANY_ID,
                TEST_HUB_ID,
                negativeQuantity
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("재고량은 0 이상이어야 합니다");
    }

    @Test
    @DisplayName("재고량이 null이면 예외가 발생한다")
    void create_WithNullQuantity_ShouldThrowException() {
        // given: null 재고량
        Integer nullQuantity = null;

        // when & then: 예외 발생
        assertThatThrownBy(() -> Product.create(
                "테스트 상품",
                Money.of(10000L),
                TEST_CATEGORY_ID,
                TEST_COMPANY_ID,
                TEST_HUB_ID,
                nullQuantity
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("재고량은 0 이상이어야 합니다");
    }

    @Test
    @DisplayName("재고량이 0이면 정상적으로 생성된다")
    void create_WithZeroQuantity_ShouldSucceed() {
        // given: 재고량 0
        Integer zeroQuantity = 0;

        // when: 상품 생성
        Product product = Product.create(
                "테스트 상품",
                Money.of(10000L),
                TEST_CATEGORY_ID,
                TEST_COMPANY_ID,
                TEST_HUB_ID,
                zeroQuantity
        );

        // then: 정상적으로 생성됨
        assertThat(product).isNotNull();
        assertThat(product.getStock().getQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("이미 삭제된 상품을 다시 삭제해도 deletedAt이 변경되지 않는다")
    void markAsDeleted_AlreadyDeleted_ShouldNotChangeDeletedAt() {
        // given: 이미 삭제된 상품
        Product product = ProductFixture.defaultProduct();
        product.markAsDeleted();
        var firstDeletedAt = product.getDeletedAt();

        // when: 다시 삭제 시도
        product.markAsDeleted();

        // then: deletedAt이 변경되지 않음
        assertThat(product.getDeletedAt()).isEqualTo(firstDeletedAt);
    }

    @Test
    @DisplayName("삭제된 상품을 복원하면 deletedAt이 null이 된다")
    void restore_DeletedProduct_ShouldClearDeletedAt() {
        // given: 삭제된 상품
        Product product = ProductFixture.deleted();
        assertThat(product.getDeletedAt()).isNotNull();

        // when: 복원
        product.restore();

        // then: deletedAt이 null이 됨
        assertThat(product.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("상품 생성 시 Stock의 companyId와 hubId가 Product와 동일하다")
    void create_ShouldCreateStockWithSameCompanyAndHub() {
        // given: 상품 생성 정보
        UUID companyId = UUID.fromString("90000000-0000-0000-0000-000000000001");
        UUID hubId = UUID.fromString("90000000-0000-0000-0000-000000000002");

        // when: 상품 생성
        Product product = Product.create(
                "테스트 상품",
                Money.of(10000L),
                TEST_CATEGORY_ID,
                companyId,
                hubId,
                100
        );

        // then: Stock의 companyId, hubId가 Product와 동일
        assertThat(product.getStock().getCompanyId()).isEqualTo(companyId);
        assertThat(product.getStock().getHubId()).isEqualTo(hubId);
    }
}
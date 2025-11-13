package org.sparta.product.support.fixtures;

import org.sparta.product.domain.entity.Product;
import org.sparta.product.domain.vo.Money;

import java.util.UUID;

/**
 * Product 테스트 픽스처
 * - Object Mother 패턴 사용
 * - 반복적으로 사용되는 Product 객체 생성
 * - 테스트 일관성을 위해 고정된 UUID 사용
 */
public final class ProductFixture {

    // 테스트용 고정 UUID
    private static final UUID DEFAULT_CATEGORY_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID DEFAULT_COMPANY_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID DEFAULT_HUB_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    private ProductFixture() {
    }

    /**
     * 기본 상품 생성
     */
    public static Product defaultProduct() {
        return Product.create(
                "테스트 상품",
                Money.of(10000L),
                DEFAULT_CATEGORY_ID,
                DEFAULT_COMPANY_ID,
                DEFAULT_HUB_ID,
                100  // initialQuantity
        );
    }

    /**
     * 특정 재고량을 가진 상품 생성
     */
    public static Product withStock(Integer quantity) {
        return Product.create(
                "재고 테스트 상품",
                Money.of(10000L),
                DEFAULT_CATEGORY_ID,
                DEFAULT_COMPANY_ID,
                DEFAULT_HUB_ID,
                quantity
        );
    }

    /**
     * 특정 가격을 가진 상품 생성
     */
    public static Product withPrice(Long price) {
        return Product.create(
                "가격 테스트 상품",
                Money.of(price),
                DEFAULT_CATEGORY_ID,
                DEFAULT_COMPANY_ID,
                DEFAULT_HUB_ID,
                100
        );
    }

    /**
     * 특정 업체의 상품 생성
     */
    public static Product withCompany(UUID companyId) {
        return Product.create(
                "업체 테스트 상품",
                Money.of(10000L),
                DEFAULT_CATEGORY_ID,
                companyId,
                DEFAULT_HUB_ID,
                100
        );
    }

    /**
     * 특정 허브의 상품 생성
     */
    public static Product withHub(UUID hubId) {
        return Product.create(
                "허브 테스트 상품",
                Money.of(10000L),
                DEFAULT_CATEGORY_ID,
                DEFAULT_COMPANY_ID,
                hubId,
                100
        );
    }

    /**
     * 특정 카테고리의 상품 생성
     */
    public static Product withCategory(UUID categoryId) {
        return Product.create(
                "카테고리 테스트 상품",
                Money.of(10000L),
                categoryId,
                DEFAULT_COMPANY_ID,
                DEFAULT_HUB_ID,
                100
        );
    }

    /**
     * 논리적 삭제된 상품 생성
     */
    public static Product deleted() {
        Product product = defaultProduct();
        product.markAsDeleted();
        return product;
    }
}
package org.sparta.order.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.common.error.BusinessException;
import org.sparta.jpa.entity.BaseEntity;
import org.sparta.order.domain.error.ProductErrorType;
import org.sparta.order.domain.vo.ProductMoney;

import java.util.UUID;

/**
 * 상품 Aggregate Root
 * - 상품 메타데이터 관리
 * - Stock과 생명주기 공유
 * - Stock의 @MapsId로 ID 공유
 */
@Entity
@Getter
@Table(name = "p_order_products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String productName;

    @Embedded
    @Column(nullable = false)
    private ProductMoney price;

    @Column(nullable = false)
    private UUID categoryId;

    @Column(nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private UUID hubId;

    @Column(nullable = false)
    private Boolean isActive = true;

    /**
     * Stock과 1:1 관계
     * - mappedBy: Stock의 product 필드와 매핑
     * - cascade: Product 저장/삭제 시 Stock도 함께
     * - orphanRemoval: Product에서 Stock 제거 시 자동 삭제
     */
    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private Stock stock;

    private Product(
            String productName,
            ProductMoney price,
            UUID categoryId,
            UUID companyId,
            UUID hubId,
            Boolean isActive
    ) {
        this.productName = productName;
        this.price = price;
        this.categoryId = categoryId;
        this.companyId = companyId;
        this.hubId = hubId;
        this.isActive = isActive != null ? isActive : true;
    }

    /**
     * 상품 생성 팩토리 메서드
     * - 필수 검증 수행
     * - Stock 생성 및 연결
     */
    public static Product create(
            String productName,
            ProductMoney price,
            UUID categoryId,
            UUID companyId,
            UUID hubId,
            Integer initialQuantity
    ) {
        validateProductName(productName);
        validatePrice(price);
        validateCategoryId(categoryId);
        validateCompanyId(companyId);
        validateHubId(hubId);
        validateInitialQuantity(initialQuantity);

        Product product = new Product(
                productName,
                price,
                categoryId,
                companyId,
                hubId,
                true
        );

        Stock stock = Stock.create(
                product,
                companyId,
                hubId,
                initialQuantity
        );

        product.stock = stock;

        return product;
    }

    private static void validateProductName(String productName) {
        if (productName == null || productName.isBlank()) {
            throw new BusinessException(ProductErrorType.PRODUCT_NAME_REQUIRED);
        }
    }

    private static void validatePrice(ProductMoney price) {
        if (price == null) {
            throw new BusinessException(ProductErrorType.PRICE_REQUIRED);
        }
    }

    private static void validateCategoryId(UUID categoryId) {
        if (categoryId == null) {
            throw new BusinessException(ProductErrorType.CATEGORY_ID_REQUIRED);
        }
    }

    private static void validateCompanyId(UUID companyId) {
        if (companyId == null) {
            throw new BusinessException(ProductErrorType.COMPANY_ID_REQUIRED);
        }
    }

    private static void validateHubId(UUID hubId) {
        if (hubId == null) {
            throw new BusinessException(ProductErrorType.HUB_ID_REQUIRED);
        }
    }

    private static void validateInitialQuantity(Integer initialQuantity) {
        if (initialQuantity == null || initialQuantity < 0) {
            throw new BusinessException(ProductErrorType.INITIAL_QUANTITY_INVALID);
        }
    }

    /**
     * 상품 정보 수정
     * - 상품명과 가격을 수정할 수 있음
     * - null인 경우 기존 값 유지
     */
    public void update(String productName, ProductMoney price) {
        if (productName != null && !productName.isBlank()) {
            this.productName = productName;
        }
        if (price != null) {
            this.price = price;
        }
    }

    /**
     * 상품 논리적 삭제
     * - isActive를 false로 설정
     * - 연관된 재고도 판매 불가 상태로 변경
     */
    public void delete() {
        this.isActive = false;
        if (this.stock != null) {
            this.stock.markAsUnavailable();
        }
    }



}
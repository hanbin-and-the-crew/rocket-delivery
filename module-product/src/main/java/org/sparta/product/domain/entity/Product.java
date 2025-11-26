package org.sparta.product.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.common.error.BusinessException;
import org.sparta.jpa.entity.BaseEntity;
import org.sparta.product.domain.error.ProductErrorType;
import org.sparta.product.domain.vo.Money;

import java.util.UUID;

/**
 * 상품 애그리거트
 * - 상품 메타데이터 관리
 * - Stock과 독립된 생명주기
 * - 생명주기 이벤트를 통해 Stock과 동기화
 */
@Entity
@Getter
@Table(name = "p_products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String productName;

    @Embedded
    @Column(nullable = false)
    private Money price;

    @Column(nullable = false)
    private UUID categoryId;

    @Column(nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private UUID hubId;

    @Column(nullable = false)
    private Boolean isActive = true;

    private Product(
            String productName,
            Money price,
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
     * - Stock은 이벤트를 통해 별도로 생성됨
     */
    public static Product create(
            String productName,
            Money price,
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

        return new Product(
                productName,
                price,
                categoryId,
                companyId,
                hubId,
                true
        );
    }

    private static void validateProductName(String productName) {
        if (productName == null || productName.isBlank()) {
            throw new BusinessException(ProductErrorType.PRODUCT_NAME_REQUIRED);
        }
    }

    private static void validatePrice(Money price) {
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
    public void update(String productName, Money price) {
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
     * - 재고는 이벤트를 통해 판매 불가 상태로 변경됨
     */
    public void delete() {
        this.isActive = false;
    }



}
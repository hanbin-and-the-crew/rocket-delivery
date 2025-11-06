package org.sparta.product.domain.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.jpa.entity.BaseEntity;
import org.sparta.product.domain.vo.Money;

import java.util.UUID;

/**
 * 상품 Aggregate Root
 * - 상품 메타데이터 관리
 * - Stock과 생명주기 공유
 * - Stock의 @MapsId로 ID 공유
 */
@Entity
@Table(name = "p_products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String productName;

    @Column(nullable = false)
    private Money price;

    @Column(nullable = false)
    private UUID categoryId;

    @Column(nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private UUID hubId;

    @Column(nullable = false)
    private Boolean isActive;

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
     * - Stock 생성 및 연결
     */
    public static Product create(
            String productName,
            Money price,
            UUID categoryId,
            UUID companyId,
            UUID hubId,
            Integer initialQuantity
    ) {
        // 1. 상품명 검증
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("상품명은 필수입니다");
        }

        // 2. 가격 검증 (Money VO에서 이미 검증됨)
        if (price == null) {
            throw new IllegalArgumentException("가격은 필수입니다");
        }

        // 3. 카테고리 검증
        if (categoryId == null) {
            throw new IllegalArgumentException("카테고리 ID는 필수입니다");
        }

        // 4. 상품 업체 존재 검증
        if (companyId == null) {
            throw new IllegalArgumentException("업체 ID는 필수입니다");
        }

        // 5. 상품 관리 허브 ID 검증
        if (hubId == null) {
            throw new IllegalArgumentException("허브 ID는 필수입니다");
        }

        // 6. 초기 재고량 검증 (0 이상)
        if (initialQuantity == null || initialQuantity < 0) {
            throw new IllegalArgumentException("재고량은 0 이상이어야 합니다");
        }

        // 7. Product 생성
        Product product = new Product(
                productName,
                price,
                categoryId,
                companyId,
                hubId,
                true
        );

        // 8. Stock 생성 및 연결
        Stock stock = Stock.builder()
                .product(product)
                .companyId(companyId)
                .hubId(hubId)
                .quantity(initialQuantity)
                .reservedQuantity(0)
                .build();

        product.stock = stock;

        return product;
    }

}
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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.jpa.entity.BaseEntity;

/**
 * 상품 Aggregate Root
 * - 상품 메타데이터 관리
 * - Stock과 생명주기 공유 (orphanRemoval)
 * - Stock의 @MapsId로 ID 공유
 */
@Entity
@Table(name = "p_products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String productName;

    @Column(nullable = false)
    private Integer price;

    @Column(nullable = false)
    private Long categoryId;

    @Column(nullable = false)
    private Long companyId;

    @Column(nullable = false)
    private Long hubId;

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

    @Builder
    private Product(String productName, Integer price, Long categoryId,
                   Long companyId, Long hubId, Boolean isActive) {
        this.productName = productName;
        this.price = price;
        this.categoryId = categoryId;
        this.companyId = companyId;
        this.hubId = hubId;
        this.isActive = isActive != null ? isActive : true;
    }

    /**
     * Stock 설정 (양방향 관계 편의 메서드)
     */
    public void setStock(Stock stock) {
        this.stock = stock;
    }
}
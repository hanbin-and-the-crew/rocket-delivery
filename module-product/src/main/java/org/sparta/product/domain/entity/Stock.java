package org.sparta.product.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.common.error.BusinessException;
import org.sparta.jpa.entity.BaseEntity;
import org.sparta.product.domain.error.ProductErrorType;

import java.util.UUID;

/**
 * 재고 엔티티
 * - Product와 생명주기 공유 (@MapsId)
 * - Product ID와 동일한 ID 사용
 * - 독립적인 수정 가능 (Product 더티체킹 방지)
 * - 낙관적 락으로 동시성 제어
 */
@Entity
@Getter
@Table(name = "p_stocks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock extends BaseEntity {

    @Id
    private UUID id;

    @MapsId
    @OneToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private UUID hubId;

    /**
     * 실물(창고 기준) 총 재고량
     */
    @Column(nullable = false)
    private Integer quantity;

    /**
     * 주문으로 예약된 재고량
     */
    @Column(nullable = false)
    private Integer reservedQuantity;

    @Version
    private Integer version;

    private Stock(Product product, UUID companyId, UUID hubId,
                 Integer quantity) {
        this.product = product;
        this.companyId = companyId;
        this.hubId = hubId;
        this.quantity = quantity;
        this.reservedQuantity =  0;
    }
    /**
     * Stock 생성 팩토리 메서드
     * - Product와 생명주기를 공유하므로 Product 필수
     */
    public static Stock create(
            Product product,
            UUID companyId,
            UUID hubId,
            Integer initialQuantity
    ) {
        validateProduct(product);
        validateCompanyId(companyId);
        validateHubId(hubId);
        validateInitialQuantity(initialQuantity);

        return new Stock(product, companyId, hubId, initialQuantity);
    }

    private static void validateProduct(Product product) {
        if (product == null) {
            throw new BusinessException(ProductErrorType.PRODUCT_REQUIRED);
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
     * 실제 주문 가능한 재고량
     * quantity - reservedQuantity
     */
    public Integer getAvailableQuantity() {
        return quantity - reservedQuantity;
    }
}
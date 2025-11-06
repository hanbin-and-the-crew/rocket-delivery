package org.sparta.product.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.jpa.entity.BaseEntity;

import java.util.UUID;

/**
 * 재고 엔티티
 * - Product와 생명주기 공유 (@MapsId)
 * - Product ID와 동일한 ID 사용
 * - 독립적인 수정 가능 (Product 더티체킹 방지)
 * - 낙관적 락으로 동시성 제어
 */
@Entity
@Table(name = "p_stocks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock extends BaseEntity {

    /**
     * Product와 같은 ID 공유
     * @MapsId로 Product의 ID를 PK로 사용
     */
    @Id
    private UUID id;

    /**
     * Product와의 1:1 관계
     * @MapsId: 이 관계의 ID를 Stock의 PK로 사용
     */
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

    /**
     * 낙관적 락을 위한 버전
     * - 동시 재고 수정 시 충돌 감지
     */
    @Version
    private Integer version;

    @Builder
    private Stock(Product product, UUID companyId, UUID hubId,
                 Integer quantity, Integer reservedQuantity) {
        this.product = product;
        this.companyId = companyId;
        this.hubId = hubId;
        this.quantity = quantity != null ? quantity : 0;
        this.reservedQuantity = reservedQuantity != null ? reservedQuantity : 0;
    }

    /**
     * 실제 주문 가능한 재고량
     * quantity - reservedQuantity
     */
    public Integer getAvailableQuantity() {
        return quantity - reservedQuantity;
    }
}
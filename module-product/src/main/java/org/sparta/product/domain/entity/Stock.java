package org.sparta.product.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.common.error.BusinessException;
import org.sparta.jpa.entity.BaseEntity;
import org.sparta.product.domain.enums.StockStatus;
import org.sparta.product.domain.error.ProductErrorType;

import java.util.UUID;

/**
 * 재고 애그리거트
 * - Product와 독립된 생명주기 관리
 * - Product 이벤트를 통해 생명주기 동기화
 * - 낙관적 락으로 동시성 제어
 */
@Entity
@Getter
@Table(
    name = "p_stocks",
    indexes = {
        @Index(name = "idx_stocks_product_id", columnList = "product_id"),
        @Index(name = "idx_stocks_company_id", columnList = "company_id")
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private UUID hubId;

    /**
     * 실물(창고 기준) 총 재고량
     */
    @Column(nullable = false)
    private int quantity;

    /**
     * 주문으로 예약된 재고량
     */
    @Column(nullable = false)
    private int reservedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StockStatus status = StockStatus.IN_STOCK;
    @Version
    private Long version;

    private Stock(UUID productId, UUID companyId, UUID hubId,
                 Integer quantity) {
        this.productId = productId;
        this.companyId = companyId;
        this.hubId = hubId;
        this.quantity = quantity;
        this.reservedQuantity =  0;
    }

    /**
     * Stock 생성 팩토리 메서드
     * - Product 이벤트를 통해 생성됨
     * - productId로 Product와 연결 (외래키 관계 없음)
     */
    public static Stock create(
            UUID productId,
            UUID companyId,
            UUID hubId,
            Integer initialQuantity
    ) {
        validateProductId(productId);
        validateCompanyId(companyId);
        validateHubId(hubId);
        validateInitialQuantity(initialQuantity);

        return new Stock(productId, companyId, hubId, initialQuantity);
    }

    private static void validateProductId(UUID productId) {
        if (productId == null) {
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

    /**
     * 재고 충분 여부 확인
     */
    public boolean hasAvailableStock(int requestedQuantity) {
        return getAvailableQuantity() >= requestedQuantity;
    }

    /**
     * 재고 차감
     * - 가용 재고에서 차감
     * - 재고가 부족하면 예외 발생
     */
    public void decrease(int quantity) {
        validateNotUnavailable();
        validateDecreaseQuantity(quantity);

        if (!hasAvailableStock(quantity)) {
            throw new BusinessException(ProductErrorType.INSUFFICIENT_STOCK);
        }

        this.quantity -= quantity;
    }

    /**
     * 재고 복원
     * - 차감된 재고를 다시 증가
     */
    public void increase(int quantity) {
        validateIncreaseQuantity(quantity);
        this.quantity += quantity;
    }

    private void validateDecreaseQuantity(int quantity) {
        if (quantity < 1) {
            throw new BusinessException(ProductErrorType.DECREASE_QUANTITY_INVALID);
        }
    }

    private void validateIncreaseQuantity(int quantity) {
        if (quantity < 1) {
            throw new BusinessException(ProductErrorType.INCREASE_QUANTITY_INVALID);
        }
    }

    /**
     * 재고 예약 (주문 생성 시)
     * - 가용 재고(quantity - reservedQuantity)를 확인
     * - 예약 가능하면 reservedQuantity 증가
     * - 상태 자동 갱신
     */
    public void reserve(int quantity) {
        validateNotUnavailable();
        validateReserveQuantity(quantity);

        if (!hasAvailableStock(quantity)) {
            throw new BusinessException(ProductErrorType.INSUFFICIENT_STOCK);
        }

        this.reservedQuantity += quantity;
        updateStatus();
    }

    /**
     * 예약 확정 (결제 완료 시)
     * - 예약된 재고를 실제 차감
     * - quantity와 reservedQuantity 모두 감소
     * - 상태 자동 갱신
     */
    public void confirmReservation(int quantity) {
        validateConfirmQuantity(quantity);

        this.quantity -= quantity;
        this.reservedQuantity -= quantity;
        updateStatus();
    }

    /**
     * 예약 취소 (주문 취소 시)
     * - 예약된 재고만 감소
     * - 실제 재고(quantity)는 유지
     * - 상태 자동 갱신
     */
    public void cancelReservation(int quantity) {
        validateCancelQuantity(quantity);

        this.reservedQuantity -= quantity;
        updateStatus();
    }

    public void restoreConfirmedReservation(int quantity) {
        validateRestoreQuantity(quantity); // quantity >= 1 같은 최소 검증 (필요시)
        this.quantity += quantity;
        updateStatus();
    }

    private void validateRestoreQuantity(int quantity) {
        if (quantity < 1) {
            throw new BusinessException(ProductErrorType.RESERVE_QUANTITY_INVALID);
        }
    }

    /**
     * 재고 상태 자동 갱신
     * - OUT_OF_STOCK: 실물 재고 0
     * - RESERVED_ONLY: 실물은 있지만 모두 예약됨
     * - IN_STOCK: 가용 재고 있음
     */
    private void updateStatus() {
        if (this.quantity == 0) {
            this.status = StockStatus.OUT_OF_STOCK;
        } else if (getAvailableQuantity() == 0) {
            this.status = StockStatus.RESERVED_ONLY;
        } else {
            this.status = StockStatus.IN_STOCK;
        }
    }

    /**
     * 재고를 판매 불가 상태로 변경
     * - 상품이 삭제되었을 때 호출
     * - UNAVAILABLE 상태에서는 예약/차감 불가
     */
    public void markAsUnavailable() {
        this.status = StockStatus.UNAVAILABLE;
    }

    /**
     * 판매 불가 상태 검증
     * - UNAVAILABLE 상태에서는 예약/차감 불가
     */
    private void validateNotUnavailable() {
        if (this.status == StockStatus.UNAVAILABLE) {
            throw new BusinessException(ProductErrorType.STOCK_UNAVAILABLE);
        }
    }

    private void validateReserveQuantity(int quantity) {
        if (quantity < 1) {
            throw new BusinessException(ProductErrorType.RESERVE_QUANTITY_INVALID);
        }
    }

    private void validateConfirmQuantity(int quantity) {
        if (quantity < 1) {
            throw new BusinessException(ProductErrorType.RESERVE_QUANTITY_INVALID);
        }
        if (this.reservedQuantity < quantity) {
            throw new BusinessException(ProductErrorType.INVALID_RESERVATION_CONFIRM);
        }
    }

    private void validateCancelQuantity(int quantity) {
        if (quantity < 1) {
            throw new BusinessException(ProductErrorType.RESERVE_QUANTITY_INVALID);
        }
        if (this.reservedQuantity < quantity) {
            throw new BusinessException(ProductErrorType.INVALID_RESERVATION_CANCEL);
        }
    }
}

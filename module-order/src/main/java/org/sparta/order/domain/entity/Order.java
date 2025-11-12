package org.sparta.order.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.jpa.entity.BaseEntity;
import org.sparta.order.domain.enumeration.CanceledReasonCode;
import org.sparta.order.domain.enumeration.OrderStatus;
import org.sparta.order.domain.error.OrderErrorType;
import org.sparta.order.domain.vo.Money;
import org.sparta.order.domain.vo.Quantity;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.sparta.order.domain.entity.OrderValidator.*;

@Entity
@Getter
@Table(name = "p_orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID deliveryId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus orderStatus = OrderStatus.PLACED;

    @Column(nullable = false)
    private UUID supplierId;

    @Column(nullable = false)
    private UUID supplierCompanyId;

    @Column(nullable = false)
    private UUID supplierHubId;

    @Column(nullable = false)
    private UUID receiptCompanyId;

    @Column(nullable = false)
    private UUID receiptHubId;

    @Embedded
    @AttributeOverride(name = "value",
            column = @Column(name = "quantity", nullable = false))
    private Quantity quantity;

    @Embedded
    @AttributeOverride(name = "amount",
            column = @Column(name = "total_price", nullable = false))
    private Money totalPrice;

    @Column(nullable = false)
    private LocalDateTime dueAt;

    @Column(length = 300)
    private String requestedMemo;

    @Column(nullable = false)
    private UUID productId;

    @Column(nullable = false, length = 200)
    private String productNameSnapshot;

    @Embedded
    @AttributeOverride(name = "amount",
            column = @Column(name = "product_price_snapshot", nullable = false))
    private Money productPriceSnapshot;

    @Column(nullable = false, length = 300)
    private String addressSnapshot;

    @Column(nullable = false)
    private String userName;

    @Column(nullable = false)
    private String userPhoneNumber;

    @Column(nullable = false)
    private String slackId;

    private LocalDateTime dispatchedAt;
    private UUID dispatchedBy;
    private UUID canceledBy;

    private LocalDateTime canceledAt;

    @Enumerated(EnumType.STRING)
    private CanceledReasonCode canceledReasonCode;

    @Column(length = 500)
    private String canceledReasonMemo;

    @Column
    private UUID deletedBy;

    private Order(
            UUID supplierId,
            UUID supplierCompanyId,
            UUID supplierHubId,
            UUID receiptCompanyId,
            UUID receiptHubId,
            UUID productId,
            String productNameSnapshot,
            Money productPriceSnapshot,
            Quantity quantity,
            String addressSnapshot,
            String userName,
            String userPhoneNumber,
            String slackId,
            LocalDateTime dueAt,
            String requestedMemo
    ) {
        this.supplierId = requireSupplierId(supplierId);
        this.supplierCompanyId = requireSupplierCompanyId(supplierCompanyId);
        this.supplierHubId = requireSupplierHubId(supplierHubId);
        this.receiptCompanyId = requireReceiptCompanyId(receiptCompanyId);
        this.receiptHubId = requireReceiptHubId(receiptHubId);
        this.productId = requireProductId(productId);
        this.productNameSnapshot = requireProductNameSnapshot(productNameSnapshot);
        this.productPriceSnapshot = requireProductPriceSnapshot(productPriceSnapshot);
        this.quantity = requireQuantity(quantity);
        this.addressSnapshot = requireAddressSnapshot(addressSnapshot);
        this.userName = requireUserName(userName);
        this.userPhoneNumber = requireUserPhoneNumber(userPhoneNumber);
        this.slackId = requireSlackId(slackId);
        this.dueAt = requireDueAt(dueAt);

        // 총액 = 단가 × 수량
        long total = this.productPriceSnapshot.getAmount() * (long) this.quantity.getValue();
        if (total < 0) { // 오버플로우 체크
            throw new IllegalArgumentException(
                    OrderErrorType.INVALID_TOTAL_PRICE.getMessage()
            );
        }
        this.totalPrice = Money.of(total);

        // 상태 기본값
        this.orderStatus = OrderStatus.PLACED;

        // 메모 정규화
        this.requestedMemo = normalizeBlankToNull(requestedMemo);
    }

    // 주문 생성
    public static Order create(
            UUID supplierId,
            UUID supplierCompanyId,
            UUID supplierHubId,
            UUID receiptCompanyId,
            UUID receiptHubId,
            UUID productId,
            String productNameSnapshot,
            Money productPriceSnapshot,
            Quantity quantity,
            String addressSnapshot,
            String userName,
            String userPhoneNumber,
            String slackId,
            LocalDateTime dueAt,
            String requestedMemo
    ) {
        return new Order(
                supplierId,
                supplierCompanyId,
                supplierHubId,
                receiptCompanyId,
                receiptHubId,
                productId,
                productNameSnapshot,
                productPriceSnapshot,
                quantity,
                addressSnapshot,
                userName,
                userPhoneNumber,
                slackId,
                dueAt,
                requestedMemo
        );
    }

    // 주문 수량 변경 (PLACED 상태에서만)
    public void changeQuantity(int newQuantity, UUID userId) {
        requireUserId(userId);

        if (this.orderStatus != OrderStatus.PLACED) {
            throw new IllegalStateException(
                    OrderErrorType.CANNOT_CHANGE_QUANTITY_AFTER_DISPATCH.getMessage()
            );
        }

        this.quantity = Quantity.of(newQuantity);
        long total = this.productPriceSnapshot.getAmount() * (long) this.quantity.getValue();
        if (total < 0) {
            throw new IllegalArgumentException(
                    OrderErrorType.INVALID_TOTAL_PRICE.getMessage()
            );
        }
        this.totalPrice = Money.of(total);
    }

    // 납기일 변경 (PLACED 상태에서만)
    public void changeDueAt(LocalDateTime newDueAt, UUID userId) {
        requireUserId(userId);

        if (this.orderStatus != OrderStatus.PLACED) {
            throw new IllegalStateException(
                    OrderErrorType.CANNOT_CHANGE_DUE_AT_AFTER_DISPATCH.getMessage()
            );
        }

        this.dueAt = requireDueAt(newDueAt);
    }

    // 요청사항 변경 (PLACED 상태에서만)
    public void changeRequestedMemo(String newMemo, UUID userId) {
        requireUserId(userId);

        if (this.orderStatus != OrderStatus.PLACED) {
            throw new IllegalStateException(
                    OrderErrorType.CANNOT_CHANGE_MEMO_AFTER_DISPATCH.getMessage()
            );
        }

        this.requestedMemo = normalizeBlankToNull(newMemo);
    }

    // 요청사항 변경 (PLACED 상태에서만)
    public void changeAddress(String newAddress, UUID userId) {
        requireUserId(userId);

        if (this.orderStatus != OrderStatus.PLACED) {
            throw new IllegalStateException(
                    OrderErrorType.CANNOT_CHANGE_MEMO_AFTER_DISPATCH.getMessage()
            );
        }

        this.requestedMemo = normalizeBlankToNull(newAddress);
    }

    // 주문 출고 처리
    public void dispatch(UUID orderId, UUID userId, LocalDateTime dispatchedAt) {
        requireOrderId(orderId);
        requireUserId(userId);
        requireDispatchedAt(dispatchedAt);

        if (this.orderStatus == OrderStatus.DISPATCHED) {
            throw new IllegalStateException(
                    OrderErrorType.ORDER_ALREADY_DISPATCHED.getMessage()
            );
        }
        if (this.orderStatus == OrderStatus.CANCELED) {
            throw new IllegalStateException(
                    OrderErrorType.CANNOT_DISPATCH_CANCELED_ORDER.getMessage()
            );
        }
        if (this.orderStatus == OrderStatus.DELIVERED) {
            throw new IllegalStateException(
                    OrderErrorType.CANNOT_DISPATCH_DELIVERED_ORDER.getMessage()
            );
        }

        this.orderStatus = OrderStatus.DISPATCHED;
        this.dispatchedAt = dispatchedAt;
        this.dispatchedBy = userId;
    }

    // 주문 취소
    public void cancel(
            UUID orderId,
            UUID userId,
            CanceledReasonCode reasonCode,
            String reasonMemo
    ) {
        // 필수값 검증
        requireOrderId(orderId);
        requireUserId(userId);

        if (reasonCode == null) {
            throw new IllegalArgumentException(
                    OrderErrorType.CANCELED_REASON_CODE_REQUIRED.getMessage()
            );
        }
        if (reasonMemo == null || reasonMemo.isBlank()) {
            throw new IllegalArgumentException(
                    OrderErrorType.CANCELED_REASON_MEMO_REQUIRED.getMessage()
            );
        }

        // 상태 검증
        if (this.orderStatus == OrderStatus.DISPATCHED) {
            throw new IllegalStateException(
                    OrderErrorType.CANNOT_CANCEL_DISPATCHED_ORDER.getMessage()
            );
        }
        if (this.orderStatus == OrderStatus.CANCELED) {
            throw new IllegalStateException(
                    OrderErrorType.ORDER_ALREADY_CANCELED.getMessage()
            );
        }
        if (this.orderStatus == OrderStatus.DELIVERED) {
            throw new IllegalStateException(
                    OrderErrorType.CANNOT_CANCEL_DELIVERED_ORDER.getMessage()
            );
        }

        // 취소 처리
        this.orderStatus = OrderStatus.CANCELED;
        this.canceledBy = requireUserId(userId);
        this.canceledAt = LocalDateTime.now();
        this.canceledReasonCode = reasonCode;
        this.canceledReasonMemo = reasonMemo.trim();
    }

    // 주문 삭제
    public void delete(UUID userId) {
        // 업무 규칙: 출고/배송완료 이후에는 삭제 금지
        if (this.orderStatus == OrderStatus.DISPATCHED || this.orderStatus == OrderStatus.DELIVERED) {
            throw new IllegalStateException("출고/배송완료 이후 주문은 삭제할 수 없습니다.");
        }

        this.deletedBy = userId;
        this.deletedAt = LocalDateTime.now();
    }

    // 배송 ID 할당
    public void setDeliveryId(UUID deliveryId) {
        requireDeliveryId(deliveryId);
        this.deliveryId = deliveryId;
    }

    // 조회 메서드들 / 안쓰는건 추후 삭제 예정
    public boolean isPlaced() {
        return this.orderStatus == OrderStatus.PLACED;
    }

    public boolean isDispatched() {
        return this.orderStatus == OrderStatus.DISPATCHED;
    }

    public boolean isCanceled() {
        return this.orderStatus == OrderStatus.CANCELED;
    }

    public boolean isCancelable() {
        return this.orderStatus == OrderStatus.PLACED;
    }

}
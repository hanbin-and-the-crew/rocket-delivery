package org.sparta.order.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.common.error.BusinessException;
import org.sparta.jpa.entity.BaseEntity;
import org.sparta.order.domain.enumeration.CanceledReasonCode;
import org.sparta.order.domain.enumeration.OrderStatus;
import org.sparta.order.domain.error.OrderErrorType;
import org.sparta.order.domain.vo.DueAtValue;
import org.sparta.order.domain.vo.Money;
import org.sparta.order.domain.vo.Quantity;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID")
    private UUID id;

    // ===== 연관 식별자 =====

    @Column(nullable = false)
    private UUID customerId;

    @Column(nullable = false)
    private UUID supplierCompanyId;

    @Column(nullable = false)
    private UUID supplierHubId;

    @Column(nullable = false)
    private UUID receiveCompanyId;

    @Column(nullable = false)
    private UUID receiveHubId;

    @Column(nullable = false)
    private UUID productId;

    // ===== VO =====

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "quantity", nullable = false)
    )
    private Quantity quantity;

    @Embedded
    @AttributeOverride(
            name = "amount",
            column = @Column(name = "total_price", nullable = false)
    )
    private Money totalPrice;

    @Embedded
    @AttributeOverride(
            name = "amount",
            column = @Column(name = "product_price_snapshot", nullable = false)
    )
    private Money productPriceSnapshot;

    @Embedded
    @AttributeOverride(
            name = "time",
            column = @Column(name = "due_at", nullable = false)
    )
    private DueAtValue dueAt;

    @Column(nullable = false)
    private String address;

    @Column(length = 300)
    private String requestMemo;

    @Column(nullable = false, length = 100)
    private String userName;

    @Column(nullable = false, length = 50)
    private String userPhoneNumber;

    @Column(length = 100)
    private String slackId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus orderStatus;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private CanceledReasonCode canceledReasonCode;

    @Column(length = 500)
    private String canceledReasonMemo;

    @Column
    private LocalDateTime canceledAt;

    // 생성 메서드
    public static Order create(
            UUID customerId,
            UUID supplierCompanyId,
            UUID supplierHubId,
            UUID receiveCompanyId,
            UUID receiveHubId,
            UUID productId,
            Long productPrice,
            int quantity,
            LocalDateTime dueAt,
            String address,
            String requestMemo,
            String userName,
            String userPhoneNumber,
            String slackId
    ) {
        validateCreateArgs(customerId, supplierCompanyId, supplierHubId,
                receiveCompanyId, receiveHubId, productId, productPrice,
                quantity, dueAt, address, userName, userPhoneNumber);

        Order o = new Order();
        o.customerId = customerId;
        o.supplierCompanyId = supplierCompanyId;
        o.supplierHubId = supplierHubId;
        o.receiveCompanyId = receiveCompanyId;
        o.receiveHubId = receiveHubId;
        o.productId = productId;

        o.productPriceSnapshot = Money.of(productPrice);
        o.quantity = Quantity.of(quantity);
        o.totalPrice = o.productPriceSnapshot.multiply(o.quantity); // Money × Quantity

        o.dueAt = DueAtValue.of(dueAt);
        o.address = address;
        o.requestMemo = requestMemo;
        o.userName = userName;
        o.userPhoneNumber = userPhoneNumber;
        o.slackId = slackId;

        o.orderStatus = OrderStatus.CREATED;

        return o;
    }

    private static void validateCreateArgs(
            UUID customerId,
            UUID supplierCompanyId,
            UUID supplierHubId,
            UUID receiveCompanyId,
            UUID receiveHubId,
            UUID productId,
            Long productPrice,
            int quantity,
            LocalDateTime dueAt,
            String address,
            String userName,
            String userPhoneNumber
    ) {
        if (customerId == null) {
            throw new BusinessException(OrderErrorType.CUSTOMER_ID_REQUIRED);
        }
        if (supplierCompanyId == null) {
            throw new BusinessException(OrderErrorType.SUPPLIER_COMPANY_ID_REQUIRED);
        }
        if (supplierHubId == null) {
            throw new BusinessException(OrderErrorType.SUPPLIER_HUB_ID_REQUIRED);
        }
        if (receiveCompanyId == null) {
            throw new BusinessException(OrderErrorType.RECEIPT_COMPANY_ID_REQUIRED);
        }
        if (receiveHubId == null) {
            throw new BusinessException(OrderErrorType.RECEIPT_HUB_ID_REQUIRED);
        }
        if (productId == null) {
            throw new BusinessException(OrderErrorType.PRODUCT_ID_REQUIRED);
        }
        if (dueAt == null) {
            throw new BusinessException(OrderErrorType.DUE_AT_REQUIRED);
        }
        if (productPrice == null) {
            throw new BusinessException(OrderErrorType.PRODUCT_PRICE_SNAPSHOT_REQUIRED);
        }
        if (quantity < 1) {
            throw new BusinessException(OrderErrorType.INVALID_QUANTITY_RANGE);
        }
        if (address == null || address.isBlank()) {
            throw new BusinessException(OrderErrorType.ADDRESS_SNAPSHOT_REQUIRED);
        }
        if (userName == null || userName.isBlank()) {
            throw new BusinessException(OrderErrorType.USERNAME_REQUIRED);
        }
        if (userPhoneNumber == null || userPhoneNumber.isBlank()) {
            throw new BusinessException(OrderErrorType.USER_PHONENUMBER_REQUIRED);
        }

    }

    // 주문 확정 메서드
    public void approve() {
        if (orderStatus == OrderStatus.CANCELED) {
            throw new BusinessException(OrderErrorType.ORDER_ALREADY_CANCELED);
        }
        if (orderStatus != OrderStatus.CREATED) {
            throw new BusinessException(OrderErrorType.CANNOT_CHANGE_NOT_CREATED_ORDER);
        }
        this.orderStatus = OrderStatus.APPROVED;
    }

    // 출고(배송 시작) 메서드
    public void markShipped() {
        if (orderStatus == OrderStatus.SHIPPED) {
            throw new BusinessException(OrderErrorType.ORDER_ALREADY_SHIPPED);
        }
        if (orderStatus == OrderStatus.CANCELED) {
            throw new BusinessException(OrderErrorType.CANNOT_SHIPPED_CANCELED_ORDER);
        }
        if (orderStatus == OrderStatus.DELIVERED) {
            throw new BusinessException(OrderErrorType.CANNOT_SHIPPED_DELIVERED_ORDER);
        }
        if (orderStatus != OrderStatus.APPROVED) {
            throw new BusinessException(OrderErrorType.CANNOT_SHIP_NOT_APPROVED_ORDER);
        }
        this.orderStatus = OrderStatus.SHIPPED;
    }

    // 배송 완료 메서드
    public void markDelivered() {
        if (orderStatus == OrderStatus.DELIVERED) {
            throw new BusinessException(OrderErrorType.ORDER_ALREADY_DELIVERED);
        }
        if (orderStatus != OrderStatus.SHIPPED) {
            throw new BusinessException(OrderErrorType.CANNOT_DELIVER_NOT_SHIPPED_ORDER);
        }
        this.orderStatus = OrderStatus.DELIVERED;
    }

    // 취소
    public void cancel(CanceledReasonCode code, String memo) {
        // 상태 검증
        if (orderStatus == OrderStatus.CANCELED) {
            throw new BusinessException(OrderErrorType.ORDER_ALREADY_CANCELED);
        }
        if (orderStatus == OrderStatus.SHIPPED) {
            throw new BusinessException(OrderErrorType.CANNOT_CANCEL_SHIPPED_ORDER);
        }
        if (orderStatus == OrderStatus.DELIVERED) {
            throw new BusinessException(OrderErrorType.CANNOT_CANCEL_DELIVERED_ORDER);
        }
        if (!(orderStatus == OrderStatus.CREATED || orderStatus == OrderStatus.APPROVED)) {
            // 혹시 다른 상태가 들어올 수 있을 때 방어
            throw new BusinessException(OrderErrorType.CANNOT_CHANGE_NOT_CREATED_ORDER);
        }

        // 사유 코드/메모 검증
        if (code == null) {
            throw new BusinessException(OrderErrorType.CANCELED_REASON_CODE_REQUIRED);
        }
        if (memo == null || memo.isBlank()) {
            throw new BusinessException(OrderErrorType.CANCELED_REASON_MEMO_REQUIRED);
        }

        this.orderStatus = OrderStatus.CANCELED;
        this.canceledReasonCode = code;
        this.canceledReasonMemo = memo;
        this.canceledAt = LocalDateTime.now();
    }

    // 삭제 가능 여부 검증 (Soft delete 전에 호출)
    public void validateDeletable() {
        if (orderStatus == OrderStatus.SHIPPED || orderStatus == OrderStatus.DELIVERED) {
            throw new BusinessException(OrderErrorType.CANNOT_DELETE_SHIPPED_OR_DELIVERED_ORDER);
        }
    }

    // ======== 수정 기능 ========

    public void changeDueAt(LocalDateTime newDueAt) {
        if (orderStatus == OrderStatus.SHIPPED || orderStatus == OrderStatus.DELIVERED) {
            throw new BusinessException(OrderErrorType.CANNOT_CHANGE_DUE_AT_AFTER_SHIPPED);
        }
        if (orderStatus != OrderStatus.CREATED) {
            throw new BusinessException(OrderErrorType.CANNOT_CHANGE_NOT_CREATED_ORDER);
        }
        this.dueAt = DueAtValue.of(newDueAt);
    }

    public void changeAddress(String newAddress) {
        if (orderStatus == OrderStatus.SHIPPED || orderStatus == OrderStatus.DELIVERED) {
            throw new BusinessException(OrderErrorType.CANNOT_CHANGE_ADDRESS_AFTER_SHIPPED);
        }
        if (orderStatus != OrderStatus.CREATED) {
            throw new BusinessException(OrderErrorType.CANNOT_CHANGE_NOT_CREATED_ORDER);
        }
        if (newAddress == null || newAddress.isBlank()) {
            throw new BusinessException(OrderErrorType.ADDRESS_SNAPSHOT_REQUIRED);
        }
        this.address = newAddress;
    }

    public void changeRequestMemo(String newMemo) {
        if (orderStatus == OrderStatus.SHIPPED || orderStatus == OrderStatus.DELIVERED) {
            throw new BusinessException(OrderErrorType.CANNOT_CHANGE_MEMO_AFTER_SHIPPED);
        }
        if (orderStatus != OrderStatus.CREATED) {
            throw new BusinessException(OrderErrorType.CANNOT_CHANGE_NOT_CREATED_ORDER);
        }
        this.requestMemo = newMemo;
    }

}

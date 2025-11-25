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
    @GeneratedValue
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

    /**
     * Create a new Order populated with the provided identifiers, pricing, quantity, and delivery/contact details.
     *
     * @param productPrice     unit price of the product in the smallest currency unit (e.g., cents)
     * @param quantity         number of units to order; must be >= 1
     * @param dueAt            scheduled delivery time for the order
     * @param requestMemo      optional customer request or note; may be null
     * @param slackId          optional Slack identifier for notifications; may be null
     * @return                 the constructed Order with status CREATED; its `productPriceSnapshot` is set from `productPrice` and `totalPrice` is computed as productPrice × quantity
     * @throws BusinessException if any required argument is missing or invalid
     */
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

    /**
     * Validates arguments required to create an Order and throws a BusinessException for any missing or invalid value.
     *
     * @param customerId         the customer's UUID
     * @param supplierCompanyId  the supplier company UUID
     * @param supplierHubId      the supplier hub UUID
     * @param receiveCompanyId   the receiving company UUID
     * @param receiveHubId       the receiving hub UUID
     * @param productId          the product UUID
     * @param productPrice       the product unit price snapshot in smallest currency unit
     * @param quantity           the requested quantity (must be >= 1)
     * @param dueAt              the requested delivery datetime
     * @param address            the delivery address (must be non-null and non-blank)
     * @param userName           the ordering user's name (must be non-null and non-blank)
     * @param userPhoneNumber    the ordering user's phone number (must be non-null and non-blank)
     *
     * @throws BusinessException if any required argument is missing or invalid. Possible error types:
     *         CUSTOMER_ID_REQUIRED,
     *         SUPPLIER_COMPANY_ID_REQUIRED,
     *         SUPPLIER_HUB_ID_REQUIRED,
     *         RECEIPT_COMPANY_ID_REQUIRED,
     *         RECEIPT_HUB_ID_REQUIRED,
     *         PRODUCT_ID_REQUIRED,
     *         DUE_AT_REQUIRED,
     *         PRODUCT_PRICE_SNAPSHOT_REQUIRED,
     *         INVALID_QUANTITY_RANGE,
     *         ADDRESS_SNAPSHOT_REQUIRED,
     *         USERNAME_REQUIRED,
     *         USER_PHONENUMBER_REQUIRED
     */
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

    /**
     * Transition the order's status from CREATED to APPROVED.
     *
     * @throws BusinessException if the order is already canceled (error type {@link OrderErrorType#ORDER_ALREADY_CANCELED})
     * @throws BusinessException if the order is not in CREATED status (error type {@link OrderErrorType#CANNOT_CHANGE_NOT_CREATED_ORDER})
     */
    public void approve() {
        if (orderStatus == OrderStatus.CANCELED) {
            throw new BusinessException(OrderErrorType.ORDER_ALREADY_CANCELED);
        }
        if (orderStatus != OrderStatus.CREATED) {
            throw new BusinessException(OrderErrorType.CANNOT_CHANGE_NOT_CREATED_ORDER);
        }
        this.orderStatus = OrderStatus.APPROVED;
    }

    /**
     * Mark the order as shipped, starting delivery.
     *
     * Validates the current order status before transition and sets the status to `SHIPPED`.
     *
     * @throws BusinessException if the transition is not allowed:
     *         - {@code OrderErrorType.ORDER_ALREADY_SHIPPED} when the order is already shipped
     *         - {@code OrderErrorType.CANNOT_SHIPPED_CANCELED_ORDER} when the order is canceled
     *         - {@code OrderErrorType.CANNOT_SHIPPED_DELIVERED_ORDER} when the order is already delivered
     *         - {@code OrderErrorType.CANNOT_SHIP_NOT_APPROVED_ORDER} when the order is not in the approved state
     */
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

    /**
     * Mark the order as delivered.
     *
     * Sets the order status to DELIVERED.
     *
     * @throws BusinessException if the order is already delivered (OrderErrorType.ORDER_ALREADY_DELIVERED)
     *         or if the order is not in the SHIPPED state (OrderErrorType.CANNOT_DELIVER_NOT_SHIPPED_ORDER)
     */
    public void markDelivered() {
        if (orderStatus == OrderStatus.DELIVERED) {
            throw new BusinessException(OrderErrorType.ORDER_ALREADY_DELIVERED);
        }
        if (orderStatus != OrderStatus.SHIPPED) {
            throw new BusinessException(OrderErrorType.CANNOT_DELIVER_NOT_SHIPPED_ORDER);
        }
        this.orderStatus = OrderStatus.DELIVERED;
    }

    /**
     * Cancel the order with a specified reason code and memo, recording the cancellation time.
     *
     * @param code  the reason code for cancellation; must not be null
     * @param memo  a non-blank explanation for the cancellation
     * @throws BusinessException if the order is already canceled (OrderErrorType.ORDER_ALREADY_CANCELED)
     * @throws BusinessException if the order has been shipped (OrderErrorType.CANNOT_CANCEL_SHIPPED_ORDER)
     * @throws BusinessException if the order has been delivered (OrderErrorType.CANNOT_CANCEL_DELIVERED_ORDER)
     * @throws BusinessException if the order is in a state that cannot be changed (OrderErrorType.CANNOT_CHANGE_NOT_CREATED_ORDER)
     * @throws BusinessException if the cancellation reason code is missing (OrderErrorType.CANCELED_REASON_CODE_REQUIRED)
     * @throws BusinessException if the cancellation memo is null or blank (OrderErrorType.CANCELED_REASON_MEMO_REQUIRED)
     */
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

    /**
     * Ensures the order is eligible for soft deletion.
     *
     * @throws BusinessException if the order's status is SHIPPED or DELIVERED (error type CANNOT_DELETE_SHIPPED_OR_DELIVERED_ORDER)
     */
    public void validateDeletable() {
        if (orderStatus == OrderStatus.SHIPPED || orderStatus == OrderStatus.DELIVERED) {
            throw new BusinessException(OrderErrorType.CANNOT_DELETE_SHIPPED_OR_DELIVERED_ORDER);
        }
    }

    /**
     * Change the order's due date.
     *
     * @param newDueAt the new due date and time for the order
     * @throws BusinessException with OrderErrorType.CANNOT_CHANGE_DUE_AT_AFTER_SHIPPED if the order is already shipped or delivered
     * @throws BusinessException with OrderErrorType.CANNOT_CHANGE_NOT_CREATED_ORDER if the order is not in the CREATED state
     */

    public void changeDueAt(LocalDateTime newDueAt) {
        if (orderStatus == OrderStatus.SHIPPED || orderStatus == OrderStatus.DELIVERED) {
            throw new BusinessException(OrderErrorType.CANNOT_CHANGE_DUE_AT_AFTER_SHIPPED);
        }
        if (orderStatus != OrderStatus.CREATED) {
            throw new BusinessException(OrderErrorType.CANNOT_CHANGE_NOT_CREATED_ORDER);
        }
        this.dueAt = DueAtValue.of(newDueAt);
    }

    /**
     * Updates the order's delivery address if the order is still in the CREATED state.
     *
     * @param newAddress the new delivery address; must be non-null and not blank
     * @throws BusinessException if the order has already been SHIPPED or DELIVERED (OrderErrorType.CANNOT_CHANGE_MEMO_AFTER_SHIPPED)
     * @throws BusinessException if the order is not in the CREATED state (OrderErrorType.CANNOT_CHANGE_NOT_CREATED_ORDER)
     * @throws BusinessException if `newAddress` is null or blank (OrderErrorType.ADDRESS_SNAPSHOT_REQUIRED)
     */
    public void changeAddress(String newAddress) {
        if (orderStatus == OrderStatus.SHIPPED || orderStatus == OrderStatus.DELIVERED) {
            throw new BusinessException(OrderErrorType.CANNOT_CHANGE_MEMO_AFTER_SHIPPED);
        }
        if (orderStatus != OrderStatus.CREATED) {
            throw new BusinessException(OrderErrorType.CANNOT_CHANGE_NOT_CREATED_ORDER);
        }
        if (newAddress == null || newAddress.isBlank()) {
            throw new BusinessException(OrderErrorType.ADDRESS_SNAPSHOT_REQUIRED);
        }
        this.address = newAddress;
    }

    /**
     * Update the order's request memo when the order is in the CREATED state.
     *
     * @param newMemo the new request memo; may be null to clear the memo
     * @throws BusinessException if the order is already SHIPPED or DELIVERED (OrderErrorType.CANNOT_CHANGE_MEMO_AFTER_SHIPPED)
     *                           or if the order is not in CREATED state (OrderErrorType.CANNOT_CHANGE_NOT_CREATED_ORDER)
     */
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
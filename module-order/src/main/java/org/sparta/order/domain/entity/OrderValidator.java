package org.sparta.order.domain.entity;

import org.sparta.order.domain.error.OrderErrorType;
import org.sparta.order.domain.vo.Money;
import org.sparta.order.domain.vo.Quantity;

import java.time.LocalDateTime;
import java.util.UUID;

public final class OrderValidator {

    private OrderValidator() {}

    // ========== 주문 생성 시 필수값 검증 ==========

    public static UUID requireSupplierId(UUID v) {
        if (v == null) {
            throw new IllegalArgumentException(
                    OrderErrorType.SUPPLIER_ID_REQUIRED.getMessage()
            );
        }
        return v;
    }

    public static UUID requireSupplierCompanyId(UUID v) {
        if (v == null) {
            throw new IllegalArgumentException(
                    OrderErrorType.SUPPLIER_COMPANY_ID_REQUIRED.getMessage()
            );
        }
        return v;
    }

    public static UUID requireSupplierHubId(UUID v) {
        if (v == null) {
            throw new IllegalArgumentException(
                    OrderErrorType.SUPPLIER_HUB_ID_REQUIRED.getMessage()
            );
        }
        return v;
    }

    public static UUID requireReceiptCompanyId(UUID v) {
        if (v == null) {
            throw new IllegalArgumentException(
                    OrderErrorType.RECEIPT_COMPANY_ID_REQUIRED.getMessage()
            );
        }
        return v;
    }

    public static UUID requireReceiptHubId(UUID v) {
        if (v == null) {
            throw new IllegalArgumentException(
                    OrderErrorType.RECEIPT_HUB_ID_REQUIRED.getMessage()
            );
        }
        return v;
    }

    public static UUID requireProductId(UUID v) {
        if (v == null) {
            throw new IllegalArgumentException(
                    OrderErrorType.PRODUCT_ID_REQUIRED.getMessage()
            );
        }
        return v;
    }

    public static String requireProductNameSnapshot(String s) {
        if (s == null || s.isBlank()) {
            throw new IllegalArgumentException(
                    OrderErrorType.PRODUCT_NAME_SNAPSHOT_REQUIRED.getMessage()
            );
        }
        return s.trim();
    }

    public static Money requireProductPriceSnapshot(Money m) {
        if (m == null) {
            throw new IllegalArgumentException(
                    OrderErrorType.PRODUCT_PRICE_SNAPSHOT_REQUIRED.getMessage()
            );
        }
        return m;
    }

    public static Quantity requireQuantity(Quantity q) {
        if (q == null) {
            throw new IllegalArgumentException(
                    OrderErrorType.QUANTITY_REQUIRED.getMessage()
            );
        }
        // 하한 검증은 Quantity.of/생성자에서 수행(<1 이면 예외 발생)
        return q;
    }

    public static String requireAddressSnapshot(String s) {
        if (s == null || s.isBlank()) {
            throw new IllegalArgumentException(
                    OrderErrorType.ADDRESS_SNAPSHOT_REQUIRED.getMessage()
            );
        }
        return s.trim();
    }

    public static String requireUserName(String s) {
        if (s == null || s.isBlank()) {
            throw new IllegalArgumentException(
                    OrderErrorType.USERNAME_REQUIRED.getMessage()
            );
        }
        return s.trim();
    }

    public static String requireUserPhoneNumber(String s) {
        if (s == null || s.isBlank()) {
            throw new IllegalArgumentException(
                    OrderErrorType.USER_PHONENUMBER_REQUIRED.getMessage()
            );
        }
        return s.trim();
    }

    public static String requireSlackId(String s) {
        if (s == null || s.isBlank()) {
            throw new IllegalArgumentException(
                    OrderErrorType.SLACK_ID_REQUIRED.getMessage()
            );
        }
        return s.trim();
    }

    public static LocalDateTime requireDueAt(LocalDateTime t) {
        if (t == null) {
            throw new IllegalArgumentException(
                    OrderErrorType.DUE_AT_REQUIRED.getMessage()
            );
        }
        // 과거 날짜 검증
        if (t.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "납품 기한은 현재 시간 이후여야 합니다"
            );
        }
        return t;
    }

    // ========== 주문 작업 시 필수값 검증 ==========

    public static UUID requireOrderId(UUID orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException(
                    OrderErrorType.ORDER_ID_REQUIRED.getMessage()
            );
        }
        return orderId;
    }

    public static UUID requireUserId(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException(
                    OrderErrorType.USER_ID_REQUIRED.getMessage()
            );
        }
        return userId;
    }

    public static UUID requireDeliveryId(UUID deliveryId) {
        if (deliveryId == null) {
            throw new IllegalArgumentException(
                    OrderErrorType.DELIVERY_ID_REQUIRED.getMessage()
            );
        }
        return deliveryId;
    }

    public static LocalDateTime requireDispatchedAt(LocalDateTime dispatchedAt) {
        if (dispatchedAt == null) {
            throw new IllegalArgumentException(
                    OrderErrorType.DISPATCHED_AT_REQUIRED.getMessage()
            );
        }
        return dispatchedAt;
    }

    // ========== 유틸리티 메서드 ==========

    public static String normalizeBlankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    // 문자열 길이 검증
    public static String requireMaxLength(String s, int maxLength, String fieldName) {
        if (s != null && s.length() > maxLength) {
            throw new IllegalArgumentException(
                    fieldName + "은(는) " + maxLength + "자를 초과할 수 없습니다"
            );
        }
        return s;
    }
}
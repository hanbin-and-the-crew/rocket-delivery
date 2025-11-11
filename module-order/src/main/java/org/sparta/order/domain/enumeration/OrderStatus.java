package org.sparta.order.domain.enumeration;

public enum OrderStatus {
    PLACED, // 주문 완료
    DISPATCHED, // 출고 완료
    CANCELED,   // 주문 취소
    DELIVERED   // 배달 완료
}

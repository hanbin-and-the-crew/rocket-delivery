package org.sparta.payment.domain.enumeration;

public enum PaymentStatus {
    REQUESTED,   // 결제 요청됨
    REFUNDED,
    PENDING,
    COMPLETED,
    FAILED,
    CANCELED     // 결제 취소됨
}

package org.sparta.order.domain.enumeration;

public enum CanceledReasonCode {
    CUSTOMER_REQUEST,   // 고객 요청
    OUT_OF_STOCK,   // 재고 없음
    ADDRESS_MISMATCH,    // 주소 불일치
    SYSTEM_FAILED   // 시스템상의 실패
}

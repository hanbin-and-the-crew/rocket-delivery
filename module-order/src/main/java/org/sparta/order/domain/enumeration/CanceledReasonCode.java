package org.sparta.order.domain.enumeration;

public enum CanceledReasonCode {

    /**
     * 고객이 직접 요청한 취소
     */
    CUSTOMER_REQUEST,

    /**
     * 재고 부족
     */
    OUT_OF_STOCK,

    /**
     * 결제/배송/연동 등 시스템 오류로 롤백한 경우
     */
    SYSTEM_FAILED,

    /**
     * 결제 승인/정산에 실패한 경우
     */
    PAYMENT_FAILED
}

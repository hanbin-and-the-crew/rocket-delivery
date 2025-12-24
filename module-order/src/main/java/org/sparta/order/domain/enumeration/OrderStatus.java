package org.sparta.order.domain.enumeration;

public enum OrderStatus {

    /**
     * 주문이 처음 생성된 상태
     * 재고 예약 / 결제 / 배송 생성 전 or 진행중일 수 있음
     */
    CREATED,

    /**
     * 재고 확정 + 결제까지 모두 성공하여
     * 비즈니스적으로 유효한 주문이 된 상태
     */
    APPROVED,

    /**
     * 배송 준비 중 / 이때부터 주문 취소 불가
     * */
    PREPARING_ORDER,

    /**
     * 출고 완료 / 배송이 시작된 상태
     * 이 시점부터는 주문 취소 불가
     */
    SHIPPED,

    /**
     * 고객 혹은 시스템 사유로 취소된 상태
     */
    CANCELED,

    /**
     * 배송까지 모두 완료된 상태
     */
    DELIVERED
}

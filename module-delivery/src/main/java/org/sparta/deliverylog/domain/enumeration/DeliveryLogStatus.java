package org.sparta.deliverylog.domain.enumeration;

// 배송 로그 상태 (Delivery와 거의 유사하지만 허브 도착 부분이 조금 다름)
public enum DeliveryLogStatus {
    CREATED,             // 생성 직후 (배송 담당자 배정전)
    HUB_WAITING,        // 허브 출발 대기 (담당자는 배정되었지만 아직 첫 leg 시작 전)
    HUB_MOVING,         // 해당 leg 이동 중
    HUB_ARRIVED,        // 해당 leg 도착 완료
    CANCELED,           // 주문 배송 배송로그 취소
}

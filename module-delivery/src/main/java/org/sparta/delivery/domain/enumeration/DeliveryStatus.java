package org.sparta.delivery.domain.enumeration;

public enum DeliveryStatus {
    HUB_WAITING,        // 허브 대기
    HUB_MOVING,         // 허브 간 이동 중
    DEST_HUB_ARRIVED,   // 목적지 허브 도착
    COMPANY_MOVING,     // 업체로 이동 시작
    DELIVERED,           // 최종 배송 완료
    CANCELED
}

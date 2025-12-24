package org.sparta.delivery.domain.enumeration;

public enum DeliveryStatus {
    CREATED,            // Delivery, DeliveryLog 생성 완료 된 직후의 상태 (담당자 배정 전)
    HUB_WAITING,        // 허브 출발 대기 (담당자는 배정 후 / 아직 첫 leg 시작 전) (주문 취소가 가능한 상태)
    HUB_MOVING,         // 허브 간 이동 중(어떤 leg라도 진행 중) (주문 취소가 불가능한 상태)
    DEST_HUB_ARRIVED,   // 목적지 허브까지 도착 완료
    COMPANY_MOVING,     // 업체 배송 담당자가 업체로 이동 중
    DELIVERED,          // 최종 배송 완료
    CANCELED            // 주문/배송 취소
}

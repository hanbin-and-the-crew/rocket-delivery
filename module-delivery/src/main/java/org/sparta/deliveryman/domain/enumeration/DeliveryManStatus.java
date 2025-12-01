package org.sparta.deliveryapplication.deliveryman.domain.enumeration;

/**
 * 배송 담당자 상태
 */
public enum DeliveryManStatus {
    WAITING,        // 배송 대기 중
    DELIVERING,     // 배송중
    OFFLINE,        // 오프라인 (활동 안하는 중 ex.휴무 등)
    DELETED         // soft deleted 된 상태 (또는 user쪽에서 InActive 처리 된 상태)
}
package org.sparta.deliverylog.domain.enumeration;

/**
 * 배송 경로 상태
 */
public enum DeliveryRouteStatus {
    WAITING,        // 허브 이동 대기중
    MOVING,         // 허브 이동중
    COMPLETED,      // 목적지 허브 도착
    CANCELED        // 취소됨
}
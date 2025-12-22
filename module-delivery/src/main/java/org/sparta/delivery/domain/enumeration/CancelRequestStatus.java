package org.sparta.delivery.domain.enumeration;

/**
 * 배송 취소 요청 상태
 * - REQUESTED: 취소 요청됨 (배송 생성 전 / 배송이 없을 때 취소 요청이 들어오는 경우)
 * - APPLIED: 취소 적용됨 (배송 취소 완료)
 */
public enum CancelRequestStatus {
    REQUESTED,
    APPLIED
}

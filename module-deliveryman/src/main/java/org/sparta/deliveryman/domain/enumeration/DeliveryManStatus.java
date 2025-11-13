package org.sparta.deliveryman.domain.enumeration;

/**
 * 배송 담당자 상태
 */
public enum DeliveryManStatus {

    /**
     * WAITING: 배송 대기 상태, 아직 업무 배치되지 않음 (업무 수행 완료 시 다시 Waiting으로 전환)
     */
    WAITING,

    /**
     * DELIVERING: 현재 배송 업무 수행 중
     */
    DELIVERING,

    /**
     * OFFLINE: 비활성 상태, 오프라인, 휴식, 휴무 등
     */
    OFFLINE,

    /**
     * INACTIVE: 삭제 상태 (논리 삭제 시 deletedAt 저장하면서 status도 Inactive로 변경)
     */
    INACTIVE
}

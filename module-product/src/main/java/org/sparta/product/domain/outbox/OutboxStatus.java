package org.sparta.product.domain.outbox;

/**
 * Outbox 이벤트 상태
 */
public enum OutboxStatus {
    READY,   // 아직 외부로 발행하지 않은 상태
    SENT,    // 정상적으로 발행 완료
    FAILED   // 발행 시도 중 오류 발생
}

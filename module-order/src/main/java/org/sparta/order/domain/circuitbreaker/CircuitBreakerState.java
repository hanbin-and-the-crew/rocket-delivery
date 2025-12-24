package org.sparta.order.domain.circuitbreaker;

/**
 * - CLOSED: 정상 상태, 모든 요청을 통과시킴
 * - OPEN: 차단 상태, 모든 요청을 즉시 실패 처리
 * - HALF_OPEN: 시험 상태, 제한된 요청만 통과시켜 서비스 회복 여부 확인
 */
public enum CircuitBreakerState {
    CLOSED,
    OPEN,
    HALF_OPEN
}
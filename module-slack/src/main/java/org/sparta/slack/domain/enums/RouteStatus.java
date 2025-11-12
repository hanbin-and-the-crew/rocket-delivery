package org.sparta.slack.domain.enums;

/**
 * 업체 배송 경로 상태
 */
public enum RouteStatus {
    PENDING,        // 경로만 등록된 상태
    ASSIGNED,       // 담당자 배정 완료
    PLANNED,        // AI/경로 계산 완료
    DISPATCHED,     // Slack 알림 발송 완료
    IN_PROGRESS,    // 배송 진행 중
    COMPLETED,      // 배송 완료
    FAILED          // 경로 계산/발송 실패
}

package org.sparta.slack.domain.enums;

/**
 * 메시지 발송 상태
 */
public enum MessageStatus {
    PENDING,  // 발송 대기
    SENT,     // 발송 완료
    FAILED    // 발송 실패
}
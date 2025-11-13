package org.sparta.slack.application.service.notification;

import java.time.LocalDateTime;

/**
 * AI 발송 시한 계산 결과 VO.
 */
public record OrderDeadlinePlanResult(
        LocalDateTime finalDeadline,
        String routeSummary,
        String reason,
        boolean fallbackUsed
) {
}

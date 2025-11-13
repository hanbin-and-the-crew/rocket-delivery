package org.sparta.slack.application.notification.dto;

import org.sparta.slack.domain.enums.UserRole;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record OrderDeadlineNotificationResult(
        UUID orderId,
        LocalDateTime finalDeadline,
        String aiReason,
        String routeSummary,
        String templateCode,
        Set<UserRole> targetRoles,
        boolean fallbackUsed
) {
}

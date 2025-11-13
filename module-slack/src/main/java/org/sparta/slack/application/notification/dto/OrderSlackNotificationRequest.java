package org.sparta.slack.application.notification.dto;

import java.util.Set;
import java.util.UUID;

import org.sparta.slack.domain.enums.UserRole;

/**
 * AI Planning 결과를 기반으로 Slack 알림 생성을 요청할 때 사용하는 DTO
 */
public record OrderSlackNotificationRequest(
        OrderSlackMessagePayload payload,
        UUID hubId,
        Set<UserRole> targetRoles,
        String templateCode
) {
}

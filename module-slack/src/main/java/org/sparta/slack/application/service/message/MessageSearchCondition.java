package org.sparta.slack.application.service.message;

import org.sparta.slack.domain.enums.MessageStatus;

import java.time.LocalDateTime;

public record MessageSearchCondition(
        String templateCode,
        MessageStatus status,
        String slackId,
        LocalDateTime sentFrom,
        LocalDateTime sentTo
) {
}

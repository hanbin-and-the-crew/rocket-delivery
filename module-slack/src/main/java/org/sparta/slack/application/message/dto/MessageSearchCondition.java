package org.sparta.slack.application.message.dto;

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

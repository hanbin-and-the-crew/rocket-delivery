package org.sparta.slack.application.dto;

public record SlackEmailMessageResponse(
        String result,
        String slackUserId,
        String slackTimestamp
) {
}

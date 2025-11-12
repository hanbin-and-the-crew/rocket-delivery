package org.sparta.slack.application.dto.message;

public record SlackEmailMessageResponse(
        String result,
        String slackUserId,
        String slackTimestamp
) {
}

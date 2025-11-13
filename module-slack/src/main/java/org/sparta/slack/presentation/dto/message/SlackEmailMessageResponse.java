package org.sparta.slack.presentation.dto.message;

public record SlackEmailMessageResponse(
        String result,
        String slackUserId,
        String slackTimestamp
) {
}

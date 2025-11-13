package org.sparta.slack.presentation;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sparta.slack.domain.enums.MessageStatus;
import org.sparta.slack.application.message.dto.MessageSearchCondition;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public class SlackMessageSearchRequest {

    public record Query(

            @Schema(description = "템플릿 코드")
            String templateCode,

            @Schema(description = "메시지 상태")
            MessageStatus status,

            @Schema(description = "Slack ID")
            String slackId,

            @Schema(description = "발송 시작 시각")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime sentFrom,

            @Schema(description = "발송 종료 시각")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime sentTo
    ) {
        public MessageSearchCondition toCondition() {
            return new MessageSearchCondition(templateCode, status, slackId, sentFrom, sentTo);
        }
    }
}

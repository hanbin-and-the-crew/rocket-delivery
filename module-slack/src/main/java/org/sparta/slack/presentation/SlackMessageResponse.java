package org.sparta.slack.presentation;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sparta.slack.domain.entity.Message;
import org.sparta.slack.domain.entity.SlackMessageDetail;
import org.sparta.slack.domain.enums.MessageStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class SlackMessageResponse {

    @Schema(description = "Slack 메시지 상세 응답")
    public record Detail(

            UUID messageId,
            String templateCode,
            String slackId,
            MessageStatus status,
            LocalDateTime sentAt,
            String messageBody,
            String threadTs
    ) {
        public static Detail from(Message message) {
            SlackMessageDetail detail = message.getSlackDetail();
            return new Detail(
                    message.getId(),
                    message.getTemplateCode(),
                    message.getRecipient().getSlackId(),
                    message.getStatus(),
                    message.getSentAt(),
                    detail != null ? detail.getMessageBody() : null,
                    detail != null ? detail.getThreadTs() : null
            );
        }
    }

    @Schema(description = "Slack 메시지 목록 응답")
    public record Summary(
            UUID messageId,
            String templateCode,
            String slackId,
            MessageStatus status,
            LocalDateTime sentAt
    ) {
        public static Summary from(Message message) {
            return new Summary(
                    message.getId(),
                    message.getTemplateCode(),
                    message.getRecipient().getSlackId(),
                    message.getStatus(),
                    message.getSentAt()
            );
        }
    }
}

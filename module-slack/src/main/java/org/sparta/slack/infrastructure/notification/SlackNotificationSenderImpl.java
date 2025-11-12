package org.sparta.slack.infrastructure.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.error.BusinessException;
import org.sparta.slack.application.port.out.SlackDmPort;
import org.sparta.slack.application.port.out.SlackNotificationSender;
import org.sparta.slack.application.service.SlackUserDirectoryService;
import org.sparta.slack.domain.entity.Message;
import org.sparta.slack.error.SlackErrorType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlackNotificationSenderImpl implements SlackNotificationSender {

    private final SlackUserDirectoryService slackUserDirectoryService;
    private final SlackDmPort slackDmPort;

    @Override
    public void send(Message message) {
        if (message.getSlackDetail() == null || !StringUtils.hasText(message.getSlackDetail().getMessageBody())) {
            throw new BusinessException(SlackErrorType.SLACK_DELIVERY_FAILED, "Slack 메시지 본문이 없습니다.");
        }

        String target = message.getRecipient().getSlackId();
        if (!StringUtils.hasText(target)) {
            throw new BusinessException(SlackErrorType.SLACK_USER_NOT_FOUND, "Slack 대상 정보가 없습니다.");
        }

        if (target.contains("@")) {
            target = slackUserDirectoryService.resolveUserId(target);
        }

        SlackDmPort.SlackDmResult result = slackDmPort.sendMessage(target, message.getSlackDetail().getMessageBody());
        if (result != null && StringUtils.hasText(result.timestamp())) {
            message.updateSlackThreadTs(result.timestamp());
        }
    }
}

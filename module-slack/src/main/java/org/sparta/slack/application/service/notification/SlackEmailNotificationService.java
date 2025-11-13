package org.sparta.slack.application.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.slack.application.port.out.SlackDmPort;
import org.sparta.slack.infrastructure.adapter.SlackUserDirectoryAdapter;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlackEmailNotificationService {

    private final SlackUserDirectoryAdapter slackUserDirectoryAdapter;
    private final SlackDmPort slackDmPort;

    public SlackEmailMessageResult sendDirectMessage(String email, String message) {
        Assert.hasText(email, "email은 필수입니다");
        Assert.hasText(message, "message는 필수입니다");

        String userId = slackUserDirectoryAdapter.resolveUserId(email);
        SlackDmPort.SlackDmResult result = slackDmPort.sendMessage(userId, message);

        log.info("Slack DM 전송 완료 - email={}, userId={}, ts={}", email, userId, result.timestamp());
        return new SlackEmailMessageResult("ok", userId, result.timestamp());
    }

    public record SlackEmailMessageResult(
            String result,
            String slackUserId,
            String slackTimestamp
    ) {
    }
}

package org.sparta.slack.infrastructure.slack;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.error.BusinessException;
import org.sparta.slack.application.port.out.SlackDmPort;
import org.sparta.slack.error.SlackErrorType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlackDmClientImpl implements SlackDmPort {

    private final SlackApiExecutor slackApiExecutor;

    @Override
    public SlackDmResult sendMessage(String channelId, String text) {
        SlackPostMessageResponse response;
        try {
            response = slackApiExecutor.postJson(
                    "/chat.postMessage",
                    new ChatPostMessageRequest(channelId, text),
                    SlackPostMessageResponse.class
            );
        } catch (SlackApiTransportException ex) {
            log.error("Slack DM 전송 실패 - channelId={}, error={}", channelId, ex.getMessage());
            throw new BusinessException(SlackErrorType.SLACK_NETWORK_ERROR, "Slack DM 전송 중 오류가 발생했습니다.");
        }

        if (response == null) {
            throw new BusinessException(SlackErrorType.SLACK_NETWORK_ERROR, "Slack DM 응답이 비었습니다.");
        }

        if (!response.ok()) {
            throw mapSlackError(response.error());
        }

        if (!StringUtils.hasText(response.ts())) {
            log.warn("Slack DM 응답에 timestamp가 없습니다. channelId={}", channelId);
        }

        return new SlackDmResult(response.ts());
    }

    private BusinessException mapSlackError(String errorCode) {
        if (!StringUtils.hasText(errorCode)) {
            return new BusinessException(SlackErrorType.SLACK_DELIVERY_FAILED, "Slack DM 전송 실패");
        }
        return switch (errorCode) {
            case "invalid_auth", "not_authed", "account_inactive" ->
                    new BusinessException(SlackErrorType.SLACK_INVALID_AUTH, "Slack 인증 정보가 올바르지 않습니다.");
            case "channel_not_found", "is_archived", "not_in_channel" ->
                    new BusinessException(SlackErrorType.SLACK_CHANNEL_NOT_FOUND, "Slack 채널을 찾을 수 없거나 권한이 없습니다.");
            case "rate_limited" ->
                    new BusinessException(SlackErrorType.SLACK_RATE_LIMITED, "Slack DM 호출 한도를 초과했습니다.");
            default ->
                    new BusinessException(SlackErrorType.SLACK_DELIVERY_FAILED, "Slack DM 전송 실패 : " + errorCode);
        };
    }

    private record ChatPostMessageRequest(String channel, String text) {
    }

    private record SlackPostMessageResponse(
            boolean ok,
            String error,
            String ts
    ) {
    }
}

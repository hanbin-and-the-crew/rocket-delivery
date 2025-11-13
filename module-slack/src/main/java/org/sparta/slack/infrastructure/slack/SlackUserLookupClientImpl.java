package org.sparta.slack.infrastructure.slack;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.error.BusinessException;
import org.sparta.slack.application.port.out.SlackUserLookupPort;
import org.sparta.slack.error.SlackErrorType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlackUserLookupClientImpl implements SlackUserLookupPort {

    private final SlackApiExecutor slackApiExecutor;

    @Override
    public SlackUser lookupUserByEmail(String email) {
        SlackUserLookupResponse response;
        try {
            response = slackApiExecutor.get(
                    "/users.lookupByEmail",
                    uriBuilder -> uriBuilder.queryParam("email", email),
                    SlackUserLookupResponse.class
            );
        } catch (SlackApiTransportException ex) {
            log.error("Slack 사용자 조회 실패 - email={}, error={}", email, ex.getMessage());
            throw new BusinessException(SlackErrorType.SLACK_NETWORK_ERROR, "Slack 사용자 조회 중 오류가 발생했습니다.");
        }

        if (response == null) {
            throw new BusinessException(SlackErrorType.SLACK_NETWORK_ERROR, "Slack 사용자 조회 응답이 비었습니다.");
        }

        if (!response.ok()) {
            throw mapSlackError(response.error(), email);
        }

        if (response.user() == null || !StringUtils.hasText(response.user().id())) {
            throw new BusinessException(SlackErrorType.SLACK_USER_NOT_FOUND, "Slack 사용자 정보를 찾을 수 없습니다.");
        }

        return new SlackUser(response.user().id(),
                response.user().profile() != null ? response.user().profile().email() : email,
                response.user().profile() != null ? response.user().profile().real_name() : null);
    }

    private BusinessException mapSlackError(String errorCode, String email) {
        if (!StringUtils.hasText(errorCode)) {
            return new BusinessException(SlackErrorType.SLACK_USER_NOT_FOUND, "Slack 사용자 정보를 찾을 수 없습니다.");
        }
        return switch (errorCode) {
            case "invalid_auth", "not_authed", "account_inactive" ->
                    new BusinessException(SlackErrorType.SLACK_INVALID_AUTH, "Slack 인증 정보가 올바르지 않습니다.");
            case "users_not_found", "user_not_found" ->
                    new BusinessException(SlackErrorType.SLACK_USER_NOT_FOUND, "해당 이메일(" + email + ") Slack 사용자를 찾을 수 없습니다.");
            case "rate_limited" ->
                    new BusinessException(SlackErrorType.SLACK_RATE_LIMITED, "Slack 사용자 조회 호출 한도를 초과했습니다.");
            default ->
                    new BusinessException(SlackErrorType.SLACK_DELIVERY_FAILED, "Slack 사용자 조회 실패 : " + errorCode);
        };
    }

    private record SlackUserLookupResponse(
            boolean ok,
            String error,
            SlackUserResponse user
    ) {
        private record SlackUserResponse(
                String id,
                SlackProfile profile
        ) {
        }

        private record SlackProfile(
                String email,
                String real_name
        ) {
        }
    }
}

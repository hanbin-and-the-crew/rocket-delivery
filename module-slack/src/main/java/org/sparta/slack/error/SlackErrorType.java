package org.sparta.slack.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sparta.common.error.ErrorType;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SlackErrorType implements ErrorType {

    USER_SLACK_VIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "SLACK-USER-001", "Slack 사용자 뷰 정보를 찾을 수 없습니다."),
    USER_SLACK_VIEW_PAYLOAD_MISSING(HttpStatus.BAD_REQUEST, "SLACK-USER-002", "필수 사용자 이벤트 데이터가 없습니다."),
    SLACK_TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "SLACK-NOTI-001", "요청된 Slack 템플릿을 찾을 수 없습니다."),
    SLACK_PAYLOAD_SERIALIZATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "SLACK-NOTI-002", "Slack 메시지 페이로드 직렬화에 실패했습니다."),
    SLACK_DELIVERY_FAILED(HttpStatus.BAD_GATEWAY, "SLACK-NOTI-003", "Slack API 전송에 실패했습니다."),
    SLACK_INVALID_AUTH(HttpStatus.UNAUTHORIZED, "SLACK-AUTH-001", "Slack 인증 정보가 유효하지 않습니다."),
    SLACK_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "SLACK-USER-003", "해당 이메일의 Slack 사용자를 찾을 수 없습니다."),
    SLACK_CHANNEL_NOT_FOUND(HttpStatus.BAD_REQUEST, "SLACK-CHANNEL-001", "Slack DM 채널을 열 수 없습니다."),
    SLACK_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "SLACK-RATE-001", "Slack API 호출 한도를 초과했습니다."),
    SLACK_NETWORK_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "SLACK-NETWORK-001", "Slack API 통신 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}

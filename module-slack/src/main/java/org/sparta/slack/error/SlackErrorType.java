package org.sparta.slack.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sparta.common.error.ErrorType;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SlackErrorType implements ErrorType {

    USER_SLACK_VIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "SLACK-USER-001", "Slack 사용자 뷰 정보를 찾을 수 없습니다."),
    USER_SLACK_VIEW_PAYLOAD_MISSING(HttpStatus.BAD_REQUEST, "SLACK-USER-002", "필수 사용자 이벤트 데이터가 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}

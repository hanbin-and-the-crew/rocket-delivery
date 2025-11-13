package org.sparta.slack.application.port.out;

/**
 * Slack 사용자 디렉터리 조회 포트
 */
public interface SlackUserLookupPort {

    SlackUser lookupUserByEmail(String email);

    record SlackUser(String id, String email, String realName) {}
}

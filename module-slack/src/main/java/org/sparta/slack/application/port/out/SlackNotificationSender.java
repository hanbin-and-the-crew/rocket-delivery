package org.sparta.slack.application.port.out;

import org.sparta.slack.domain.entity.Message;

/**
 * Slack 메시지를 외부 채널(API)로 전달하는 책임을 추상화한 포트
 */
public interface SlackNotificationSender {

    /**
     * 메시지를 외부 채널로 즉시 전송합니다.
     */
    void send(Message message);
}

package org.sparta.slack.application.port.out;

/**
 * Slack DM 전송 포트
 */
public interface SlackDmPort {

    SlackDmResult sendMessage(String channelId, String text);

    record SlackDmResult(String timestamp) {}
}

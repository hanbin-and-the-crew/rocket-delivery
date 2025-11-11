package org.sparta.slack.domain.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 수신자 정보 Value Object (Slack 전용)
 */
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recipient {

    @Column(name = "slack_id", nullable = false)
    private String slackId;

    private Recipient(String slackId) {
        this.slackId = slackId;
    }

    public static Recipient of(String slackId) {
        if (slackId == null || slackId.isBlank()) {
            throw new IllegalArgumentException("Slack ID는 필수입니다");
        }
        return new Recipient(slackId);
    }
}
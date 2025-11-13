package org.sparta.slack.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.common.error.BusinessException;
import org.sparta.slack.error.SlackErrorType;

import java.util.UUID;

/**
 * Slack 전용 상세 메시지 정보
 * Message Aggregate의 일부
 */
@Entity
@Getter
@Table(name = "p_slack_message_details")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SlackMessageDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "receiver_slack_id", nullable = false)
    private String receiverSlackId;

    @Column(name = "message_body", nullable = false, length = 4000)
    private String messageBody;

    @Column(name = "channel_name")
    private String channelName;

    @Column(name = "thread_ts")
    private String threadTs;

    private SlackMessageDetail(
            String receiverSlackId,
            String messageBody,
            String channelName,
            String threadTs
    ) {
        this.receiverSlackId = receiverSlackId;
        this.messageBody = messageBody;
        this.channelName = channelName;
        this.threadTs = threadTs;
    }

    static SlackMessageDetail create(
            String receiverSlackId,
            String messageBody
    ) {
        if (receiverSlackId == null || receiverSlackId.isBlank()) {
            throw new BusinessException(SlackErrorType.SLACK_INVALID_ARGUMENT, "수신자 Slack ID는 필수입니다");
        }
        if (messageBody == null || messageBody.isBlank()) {
            throw new BusinessException(SlackErrorType.SLACK_INVALID_ARGUMENT, "메시지 본문은 필수입니다");
        }

        return new SlackMessageDetail(receiverSlackId, messageBody, null, null);
    }

    static SlackMessageDetail create(
            String receiverSlackId,
            String messageBody,
            String channelName,
            String threadTs
    ) {
        if (receiverSlackId == null || receiverSlackId.isBlank()) {
            throw new BusinessException(SlackErrorType.SLACK_INVALID_ARGUMENT, "수신자 Slack ID는 필수입니다");
        }
        if (messageBody == null || messageBody.isBlank()) {
            throw new BusinessException(SlackErrorType.SLACK_INVALID_ARGUMENT, "메시지 본문은 필수입니다");
        }

        return new SlackMessageDetail(receiverSlackId, messageBody, channelName, threadTs);
    }

    public void updateThreadTs(String threadTs) {
        this.threadTs = threadTs;
    }

    public void updateMessageBody(String messageBody) {
        if (messageBody == null || messageBody.isBlank()) {
            throw new BusinessException(SlackErrorType.SLACK_INVALID_ARGUMENT, "메시지 본문은 필수입니다");
        }
        this.messageBody = messageBody;
    }
}

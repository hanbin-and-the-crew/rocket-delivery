package org.sparta.slack.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.jpa.entity.BaseEntity;
import org.sparta.slack.domain.enums.Channel;
import org.sparta.slack.domain.enums.MessageStatus;
import org.sparta.slack.domain.vo.DeliveryResult;
import org.sparta.slack.domain.vo.Recipient;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Message Aggregate Root (Notification Context)
 * Slack 메시지 발송/저장/리포트 책임
 */
@Entity
@Getter
@Table(name = "p_messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Message extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Channel channel;

    @Embedded
    private Recipient recipient;

    @Column(name = "template_code", nullable = false)
    private String templateCode;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload; // JSON 형식으로 저장

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageStatus status;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Embedded
    private DeliveryResult deliveryResult;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "slack_detail_id")
    private SlackMessageDetail slackDetail;

    private Message(
            Channel channel,
            Recipient recipient,
            String templateCode,
            String payload,
            SlackMessageDetail slackDetail
    ) {
        this.channel = channel;
        this.recipient = recipient;
        this.templateCode = templateCode;
        this.payload = payload;
        this.status = MessageStatus.PENDING;
        this.deliveryResult = DeliveryResult.success();
        this.slackDetail = slackDetail;
    }

    /**
     * Slack 메시지 생성 팩토리 메서드
     */
    public static Message create(
            String slackId,
            String templateCode,
            String payload,
            String messageBody
    ) {
        validateTemplateCode(templateCode);
        validatePayload(payload);

        Recipient recipient = Recipient.of(slackId);
        SlackMessageDetail slackDetail = SlackMessageDetail.create(slackId, messageBody);

        return new Message(
                Channel.SLACK,
                recipient,
                templateCode,
                payload,
                slackDetail
        );
    }

    private static void validateTemplateCode(String templateCode) {
        if (templateCode == null || templateCode.isBlank()) {
            throw new IllegalArgumentException("템플릿 코드는 필수입니다");
        }
    }

    private static void validatePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("페이로드는 필수입니다");
        }
    }

    /**
     * 메시지 발송 성공 처리
     */
    public void markAsSent() {
        this.status = MessageStatus.SENT;
        this.sentAt = LocalDateTime.now();
        this.deliveryResult = DeliveryResult.success();
    }

    /**
     * 메시지 발송 실패 처리
     */
    public void markAsFailed(String errorCode, String errorMessage) {
        this.status = MessageStatus.FAILED;
        this.deliveryResult = DeliveryResult.failure(errorCode, errorMessage);
    }

    /**
     * Slack 메시지 상세 업데이트
     */
    public void updateSlackThreadTs(String threadTs) {
        if (this.channel != Channel.SLACK) {
            throw new IllegalStateException("Slack 채널이 아닙니다");
        }
        if (this.slackDetail != null) {
            this.slackDetail.updateThreadTs(threadTs);
        }
    }

    /**
     * 재발송 가능 여부 확인
     */
    public boolean canResend() {
        return this.status == MessageStatus.FAILED;
    }

    /**
     * 재발송 처리
     */
    public void resend() {
        if (!canResend()) {
            throw new IllegalStateException("재발송할 수 없는 상태입니다");
        }
        this.status = MessageStatus.PENDING;
        this.sentAt = null;
        this.deliveryResult = DeliveryResult.success();
    }
}
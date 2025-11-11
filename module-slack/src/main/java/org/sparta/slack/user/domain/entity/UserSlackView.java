package org.sparta.slack.user.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta.jpa.entity.BaseEntity;
import org.sparta.slack.user.domain.enums.UserRole;
import org.sparta.slack.user.domain.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User 컨텍스트 데이터를 Slack 컨텍스트로 투영한 Read Model
 */
@Getter
@Entity
@Table(
        name = "p_user_slack_view",
        indexes = {
                @Index(name = "idx_user_slack_view_slack_id", columnList = "slack_id"),
                @Index(name = "idx_user_slack_view_role", columnList = "role")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSlackView extends BaseEntity {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "user_name", length = 100, nullable = false)
    private String userName;

    @Column(name = "real_name", length = 100, nullable = false)
    private String realName;

    @Column(name = "slack_id", length = 200)
    private String slackId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 50, nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private UserStatus status;

    @Column(name = "hub_id", nullable = false)
    private UUID hubId;

    @Column(name = "last_event_at", nullable = false)
    private LocalDateTime lastEventAt;

    private UserSlackView(
            UUID userId,
            String userName,
            String realName,
            String slackId,
            UserRole role,
            UserStatus status,
            UUID hubId,
            LocalDateTime eventTime
    ) {
        this.userId = userId;
        this.userName = userName;
        this.realName = realName;
        this.slackId = slackId;
        this.role = role;
        this.status = status;
        this.hubId = hubId;
        this.lastEventAt = eventTime;
    }

    public static UserSlackView create(
            UUID userId,
            String userName,
            String realName,
            String slackId,
            UserRole role,
            UserStatus status,
            UUID hubId,
            LocalDateTime eventTime
    ) {
        return new UserSlackView(
                userId,
                userName,
                realName,
                slackId,
                role,
                status,
                hubId,
                eventTime
        );
    }

    public void apply(
            String userName,
            String realName,
            String slackId,
            UserRole role,
            UserStatus status,
            UUID hubId,
            LocalDateTime eventTime
    ) {
        if (!shouldApply(eventTime)) {
            return;
        }

        this.userName = userName;
        this.realName = realName;
        this.slackId = slackId;
        this.role = role;
        this.status = status;
        this.hubId = hubId;
        this.lastEventAt = eventTime;
        restore();
    }

    public void markDeleted(LocalDateTime eventTime) {
        if (!shouldApply(eventTime)) {
            return;
        }
        this.lastEventAt = eventTime;
        markAsDeleted();
    }

    private boolean shouldApply(LocalDateTime eventTime) {
        return lastEventAt == null || !eventTime.isBefore(lastEventAt);
    }
}

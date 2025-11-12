package org.sparta.slack.support.fixture;

import org.sparta.slack.domain.entity.UserSlackView;
import org.sparta.slack.domain.enums.UserRole;
import org.sparta.slack.domain.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Slack 사용자 뷰 생성용 Fixture.
 */
public final class UserSlackViewFixture {

    private UserSlackViewFixture() {
    }

    public static UserSlackView approvedManager(UUID userId, UUID hubId, UserRole role, String slackId) {
        return UserSlackView.create(
                userId,
                "manager-" + role.name().toLowerCase(),
                "홍길동",
                slackId,
                role,
                UserStatus.APPROVE,
                hubId,
                LocalDateTime.now()
        );
    }
}

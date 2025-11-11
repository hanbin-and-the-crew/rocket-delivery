package org.sparta.slack.user.domain.repository;

import org.sparta.slack.user.domain.entity.UserSlackView;

import java.util.Optional;
import java.util.UUID;

/**
 * UserSlackView 도메인 저장소 (DIP 유지)
 */
public interface UserSlackViewRepository {

    UserSlackView save(UserSlackView userSlackView);

    Optional<UserSlackView> findById(UUID userId);

    Optional<UserSlackView> findByUserId(UUID userId);

    Optional<UserSlackView> findBySlackId(String slackId);
}

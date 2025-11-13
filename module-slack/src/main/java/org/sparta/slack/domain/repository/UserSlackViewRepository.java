package org.sparta.slack.domain.repository;

import org.sparta.slack.domain.entity.UserSlackView;
import org.sparta.slack.domain.enums.UserRole;
import org.sparta.slack.domain.enums.UserStatus;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * UserSlackView 도메인 저장소 (DIP 유지)
 */
public interface UserSlackViewRepository {

    UserSlackView save(UserSlackView userSlackView);

    Optional<UserSlackView> findById(UUID userId);

    Optional<UserSlackView> findByUserId(UUID userId);

    Optional<UserSlackView> findBySlackId(String slackId);

    List<UserSlackView> findAllByRolesAndStatus(Set<UserRole> roles, UserStatus status);

    List<UserSlackView> findAllByHubIdAndRolesAndStatus(UUID hubId, Set<UserRole> roles, UserStatus status);
}

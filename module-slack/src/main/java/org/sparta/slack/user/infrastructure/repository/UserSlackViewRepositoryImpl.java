package org.sparta.slack.user.infrastructure.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.sparta.slack.user.domain.entity.UserSlackView;
import org.sparta.slack.user.domain.enums.UserRole;
import org.sparta.slack.user.domain.enums.UserStatus;
import org.sparta.slack.user.domain.repository.UserSlackViewRepository;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserSlackViewRepositoryImpl implements UserSlackViewRepository {

    private final UserSlackViewJpaRepository userSlackViewJpaRepository;

    @Override
    public UserSlackView save(UserSlackView userSlackView) {
        return userSlackViewJpaRepository.save(userSlackView);
    }

    @Override
    public Optional<UserSlackView> findById(UUID userId) {
        return userSlackViewJpaRepository.findById(userId);
    }

    @Override
    public Optional<UserSlackView> findByUserId(UUID userId) {
        return userSlackViewJpaRepository.findByUserId(userId);
    }

    @Override
    public Optional<UserSlackView> findBySlackId(String slackId) {
        return userSlackViewJpaRepository.findBySlackId(slackId);
    }

    @Override
    public List<UserSlackView> findAllByRolesAndStatus(Set<UserRole> roles, UserStatus status) {
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }
        return userSlackViewJpaRepository.findAllByRoleInAndStatusAndDeletedAtIsNull(roles, status);
    }

    @Override
    public List<UserSlackView> findAllByHubIdAndRolesAndStatus(UUID hubId, Set<UserRole> roles, UserStatus status) {
        if (hubId == null || roles == null || roles.isEmpty()) {
            return List.of();
        }
        return userSlackViewJpaRepository.findAllByHubIdAndRoleInAndStatusAndDeletedAtIsNull(hubId, roles, status);
    }
}

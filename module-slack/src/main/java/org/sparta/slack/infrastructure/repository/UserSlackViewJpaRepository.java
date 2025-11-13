package org.sparta.slack.infrastructure.repository;

import org.sparta.slack.domain.entity.UserSlackView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.sparta.slack.domain.enums.UserRole;
import org.sparta.slack.domain.enums.UserStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSlackViewJpaRepository extends JpaRepository<UserSlackView, UUID> {

    Optional<UserSlackView> findByUserId(UUID userId);

    Optional<UserSlackView> findBySlackId(String slackId);

    List<UserSlackView> findAllByRoleInAndStatusAndDeletedAtIsNull(Collection<UserRole> roles, UserStatus status);

    List<UserSlackView> findAllByHubIdAndRoleInAndStatusAndDeletedAtIsNull(UUID hubId, Collection<UserRole> roles, UserStatus status);
}

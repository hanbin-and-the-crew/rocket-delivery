package org.sparta.slack.user.infrastructure.repository;

import org.sparta.slack.user.domain.entity.UserSlackView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSlackViewJpaRepository extends JpaRepository<UserSlackView, UUID> {

    Optional<UserSlackView> findByUserId(UUID userId);

    Optional<UserSlackView> findBySlackId(String slackId);
}

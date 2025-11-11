package org.sparta.slack.user.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.sparta.slack.user.domain.entity.UserSlackView;
import org.sparta.slack.user.domain.repository.UserSlackViewRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

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
}

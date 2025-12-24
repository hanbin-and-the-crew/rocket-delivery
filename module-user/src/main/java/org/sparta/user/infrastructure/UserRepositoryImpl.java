package org.sparta.user.infrastructure;

import lombok.RequiredArgsConstructor;
import org.sparta.user.domain.entity.User;
import org.sparta.user.domain.repository.UserRepository;
import org.sparta.user.infrastructure.jpa.UserJpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    @Override
    public User save(User user) {
        return userJpaRepository.save(user);
    }

    @Override
    public List<User> findAll() {
        return userJpaRepository.findAll();
    }

    @Override
    public Optional<User> findByUserId(UUID userId) {
        return userJpaRepository.findByUserId(userId);
    }

    @Override
    public Optional<User> findByUserName(String userName) {
        return userJpaRepository.findByUserName(userName);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email);
    }

    @Override
    public Optional<User> findBySlackId(String slackId) {
        return userJpaRepository.findBySlackId(slackId);
    }

    @Override
    public int softDeleteByUserId(UUID userId, LocalDateTime deletedAt) {
        return userJpaRepository.softDeleteByUserId(userId, deletedAt);
    }

    @Override
    public void deleteAll() {
        userJpaRepository.deleteAll();
    }

    @Override
    public long count() {
        return userJpaRepository.count();
    }
}

package org.sparta.user.domain.repository;

import org.sparta.user.domain.entity.User;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository  {
    User save(User user);
    List<User> findAll();
    Optional<User> findByUserId(UUID userId);
    Optional<User> findByUserName(String userName);
    Optional<User> findByEmail(String email);
    Optional<User> findBySlackId(String slackId);
    void deleteAll();

    int softDeleteByUserId(@Param("userId") UUID userId,
                           @Param("deletedAt") LocalDateTime deletedAt);
}

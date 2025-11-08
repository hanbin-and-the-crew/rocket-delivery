package org.sparta.user.domain.repository;

import org.sparta.user.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUserName(String userName);
    Optional<User> findByEmail(String email);
    Optional<User> findBySlackId(String slackId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
       update User u
          set u.deletedAt = :deletedAt
        where u.userId = :userId
          and u.deletedAt is null
       """)
    int softDeleteByUserId(@Param("userId") UUID userId,
                           @Param("deletedAt") LocalDateTime deletedAt);

}

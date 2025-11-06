package org.sparta.user.domain.repository;

import org.sparta.user.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUserName(String username);
    //Optional<User> findBySlackId(String slackId);
}

package org.sparta.slack.infrastructure.repository;

import java.util.UUID;

import org.sparta.slack.domain.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageJpaRepository extends JpaRepository<Message, UUID>, JpaSpecificationExecutor<Message> {
}

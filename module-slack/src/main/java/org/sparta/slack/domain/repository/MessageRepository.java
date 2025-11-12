package org.sparta.slack.domain.repository;

import org.sparta.slack.domain.entity.Message;

import java.util.Optional;
import java.util.UUID;

/**
 * Message Aggregate를 위한 저장소 추상화
 */
public interface MessageRepository {

    Message save(Message message);

    Optional<Message> findById(UUID id);
}

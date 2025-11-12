package org.sparta.slack.infrastructure.repository;

import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.sparta.slack.domain.entity.Message;
import org.sparta.slack.domain.repository.MessageRepository;
import org.springframework.stereotype.Repository;

/**
 * Message 저장소 구현체
 */
@Repository
@RequiredArgsConstructor
public class MessageRepositoryImpl implements MessageRepository {

    private final MessageJpaRepository messageJpaRepository;

    @Override
    public Message save(Message message) {
        return messageJpaRepository.save(message);
    }

    @Override
    public Optional<Message> findById(UUID id) {
        return messageJpaRepository.findById(id);
    }
}

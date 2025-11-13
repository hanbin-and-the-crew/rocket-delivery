package org.sparta.slack.infrastructure.repository;

import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.sparta.slack.domain.entity.Message;
import org.sparta.slack.domain.repository.MessageRepository;
import org.sparta.slack.application.message.dto.MessageSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

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

    @Override
    public Page<Message> search(MessageSearchCondition condition, Pageable pageable) {
        Specification<Message> spec = Specification.where(null);
        if (condition != null) {
            if (StringUtils.hasText(condition.templateCode())) {
                spec = spec.and((root, query, cb) ->
                        cb.like(cb.lower(root.get("templateCode")), "%" + condition.templateCode().toLowerCase() + "%"));
            }
            if (condition.status() != null) {
                spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), condition.status()));
            }
            if (StringUtils.hasText(condition.slackId())) {
                spec = spec.and((root, query, cb) ->
                        cb.equal(root.get("recipient").get("slackId"), condition.slackId()));
            }
            if (condition.sentFrom() != null) {
                spec = spec.and((root, query, cb) ->
                        cb.greaterThanOrEqualTo(root.get("sentAt"), condition.sentFrom()));
            }
            if (condition.sentTo() != null) {
                spec = spec.and((root, query, cb) ->
                        cb.lessThanOrEqualTo(root.get("sentAt"), condition.sentTo()));
            }
        }
        return messageJpaRepository.findAll(spec, pageable);
    }

    @Override
    public void delete(Message message) {
        messageJpaRepository.delete(message);
    }
}

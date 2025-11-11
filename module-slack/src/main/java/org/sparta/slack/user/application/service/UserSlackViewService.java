package org.sparta.slack.user.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.error.BusinessException;
import org.sparta.slack.error.SlackErrorType;
import org.sparta.slack.user.application.dto.UserDomainEvent;
import org.sparta.slack.user.domain.entity.UserSlackView;
import org.sparta.slack.user.domain.repository.UserSlackViewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSlackViewService {

    private final UserSlackViewRepository userSlackViewRepository;

    @Transactional
    public void sync(UserDomainEvent event) {
        if (!event.hasPayload()) {
            throw new BusinessException(SlackErrorType.USER_SLACK_VIEW_PAYLOAD_MISSING);
        }

        UserDomainEvent.Payload payload = event.payload();
        userSlackViewRepository.findById(payload.userId())
                .ifPresentOrElse(
                        entity -> entity.apply(
                                payload.userName(),
                                payload.realName(),
                                payload.slackId(),
                                payload.role(),
                                payload.status(),
                                payload.hubId(),
                                event.eventTime()
                        ),
                        () -> userSlackViewRepository.save(
                                UserSlackView.create(
                                        payload.userId(),
                                        payload.userName(),
                                        payload.realName(),
                                        payload.slackId(),
                                        payload.role(),
                                        payload.status(),
                                        payload.hubId(),
                                        event.eventTime()
                                )
                        )
                );
    }

    @Transactional
    public void delete(UserDomainEvent event) {
        if (!event.hasPayload()) {
            throw new BusinessException(SlackErrorType.USER_SLACK_VIEW_PAYLOAD_MISSING);
        }

        UUID userId = event.payload().userId();
        userSlackViewRepository.findById(userId)
                .ifPresentOrElse(
                        entity -> entity.markDeleted(event.eventTime()),
                        () -> log.debug("이미 삭제된 사용자 뷰입니다. userId={}", userId)
                );
    }
}

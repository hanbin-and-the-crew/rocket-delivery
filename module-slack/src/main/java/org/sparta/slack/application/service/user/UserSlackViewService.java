package org.sparta.slack.application.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.common.event.slack.UserDomainEvent;
import org.sparta.slack.application.mapper.UserEventPayloadMapper;
import org.sparta.slack.application.mapper.UserEventPayloadMapper.UserEventPayload;
import org.sparta.slack.domain.entity.UserSlackView;
import org.sparta.slack.domain.repository.UserSlackViewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserSlackViewService {

    private final UserSlackViewRepository userSlackViewRepository;
    private final UserEventPayloadMapper userEventPayloadMapper;

    @Transactional
    public void sync(UserDomainEvent event) {
        UserEventPayload payload = userEventPayloadMapper.map(event);

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
        UserEventPayload payload = userEventPayloadMapper.map(event);
        UUID userId = payload.userId();
        userSlackViewRepository.findById(userId)
                .ifPresentOrElse(
                        entity -> entity.markDeleted(event.eventTime()),
                        () -> log.debug("이미 삭제된 사용자 뷰입니다. userId={}", userId)
                );
    }
}

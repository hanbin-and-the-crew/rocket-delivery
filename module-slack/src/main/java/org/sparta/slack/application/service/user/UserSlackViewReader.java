package org.sparta.slack.application.service.user;

import lombok.RequiredArgsConstructor;
import org.sparta.common.error.BusinessException;
import org.sparta.slack.error.SlackErrorType;
import org.sparta.slack.domain.entity.UserSlackView;
import org.sparta.slack.domain.repository.UserSlackViewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserSlackViewReader {

    private final UserSlackViewRepository userSlackViewRepository;

    @Transactional(readOnly = true)
    public UserSlackView getByUserId(UUID userId) {
        return userSlackViewRepository.findByUserId(userId)
                .filter(entity -> entity.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(SlackErrorType.USER_SLACK_VIEW_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public UserSlackView getBySlackId(String slackId) {
        return userSlackViewRepository.findBySlackId(slackId)
                .filter(entity -> entity.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(SlackErrorType.USER_SLACK_VIEW_NOT_FOUND));
    }
}

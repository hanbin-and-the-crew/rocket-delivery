package org.sparta.slack.application.service.message;

import lombok.RequiredArgsConstructor;
import org.sparta.common.error.BusinessException;
import org.sparta.slack.presentation.PageResponse;
import org.sparta.slack.presentation.SlackMessageRequest;
import org.sparta.slack.presentation.SlackMessageResponse;
import org.sparta.slack.presentation.SlackMessageSearchRequest;
import org.sparta.slack.domain.entity.Message;
import org.sparta.slack.domain.repository.MessageRepository;
import org.sparta.slack.error.SlackErrorType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SlackMessageQueryService {

    private final MessageRepository messageRepository;

    public SlackMessageResponse.Detail get(UUID messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(SlackErrorType.SLACK_MESSAGE_NOT_FOUND));
        return SlackMessageResponse.Detail.from(message);
    }

    public PageResponse<SlackMessageResponse.Summary> search(
            SlackMessageSearchRequest.Query request,
            Pageable pageable
    ) {
        Page<Message> page = messageRepository.search(request.toCondition(), pageable);
        List<SlackMessageResponse.Summary> summaries = page.getContent().stream()
                .map(SlackMessageResponse.Summary::from)
                .toList();
        return PageResponse.from(page, summaries);
    }
}

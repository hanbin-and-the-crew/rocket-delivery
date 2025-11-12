package org.sparta.slack.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sparta.slack.application.service.message.SlackMessageQueryService;
import org.sparta.slack.domain.entity.Message;
import org.sparta.slack.domain.enums.MessageStatus;
import org.sparta.slack.domain.repository.MessageRepository;
import org.sparta.slack.presentation.PageResponse;
import org.sparta.slack.presentation.SlackMessageResponse;
import org.sparta.slack.presentation.SlackMessageSearchRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** SlackMessageQueryService의 검색/페이지 동작을 검증한다. */
@ExtendWith(MockitoExtension.class)
class SlackMessageQueryServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private SlackMessageQueryService queryService;

    @Test
    @DisplayName("검색 조건과 페이지 정보를 받아 메시지 목록을 반환한다")
    void search_ShouldReturnPagedMessages() {
        Message message = Message.create(
                "SLACK123",
                "ORDER_DEADLINE_ALERT",
                "{\"order\":\"1\"}",
                "본문"
        );
        message.markAsSent();
        Page<Message> page = new PageImpl<>(List.of(message), PageRequest.of(0, 20), 1);
        when(messageRepository.search(any(), any())).thenReturn(page);

        PageResponse<SlackMessageResponse.Summary> response = queryService.search(
                new SlackMessageSearchRequest.Query(null, MessageStatus.SENT, "SLACK123", null, null),
                PageRequest.of(0, 20)
        );

        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
    }
}

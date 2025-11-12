package org.sparta.slack.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sparta.slack.presentation.SlackMessageRequest;
import org.sparta.slack.application.port.out.SlackNotificationSender;
import org.sparta.slack.application.port.out.SlackTemplateRepository;
import org.sparta.slack.application.service.message.SlackMessageCommandService;
import org.sparta.slack.domain.entity.Message;
import org.sparta.slack.domain.entity.Template;
import org.sparta.slack.domain.enums.Channel;
import org.sparta.slack.domain.enums.TemplateFormat;
import org.sparta.slack.domain.repository.MessageRepository;
import org.sparta.slack.presentation.SlackMessageRequest;
import org.springframework.util.CollectionUtils;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/** SlackMessageCommandService의 생성/수정/삭제 동작을 검증한다. */
@ExtendWith(MockitoExtension.class)
class SlackMessageCommandServiceTest {

    @Mock
    private MessageRepository messageRepository;
    @Mock
    private SlackTemplateRepository slackTemplateRepository;
    @Mock
    private SlackNotificationSender slackNotificationSender;

    private SlackMessageCommandService commandService;

    @BeforeEach
    void setUp() {
        commandService = new SlackMessageCommandService(
                messageRepository,
                slackTemplateRepository,
                slackNotificationSender,
                new ObjectMapper()
        );
    }

    @Test
    @DisplayName("메시지를 생성하면 Slack 전송 후 저장된다")
    void create_ShouldSendAndPersistMessage() {
        SlackMessageRequest.Create request = new SlackMessageRequest.Create(
                "SLACK123",
                "ORDER_DEADLINE_ALERT",
                Map.of("orderNumber", "ORD-1")
        );
        Template template = Template.create(
                "ORDER_DEADLINE_ALERT",
                TemplateFormat.MARKDOWN,
                "주문 {{payload.orderNumber}}",
                Channel.SLACK,
                "desc"
        );
        given(slackTemplateRepository.findActiveByCode("ORDER_DEADLINE_ALERT"))
                .willReturn(Optional.of(template));
        given(messageRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        commandService.create(request);

        verify(slackNotificationSender).send(any(Message.class));
        verify(messageRepository, times(2)).save(any(Message.class));
    }

    @Test
    @DisplayName("메시지 내용을 수정하면 템플릿과 페이로드가 업데이트된다")
    void update_ShouldChangePayload() {
        UUID messageId = UUID.randomUUID();
        Message existing = Message.create(
                "SLACK123",
                "ROUTE_DAILY_SUMMARY",
                "{\"key\":\"value\"}",
                "본문"
        );
        given(messageRepository.findById(messageId)).willReturn(Optional.of(existing));
        Template template = Template.create(
                "ROUTE_DAILY_SUMMARY",
                TemplateFormat.MARKDOWN,
                "템플릿 {{data.value}}",
                Channel.SLACK,
                "route"
        );
        given(slackTemplateRepository.findActiveByCode("ROUTE_DAILY_SUMMARY"))
                .willReturn(Optional.of(template));
        given(messageRepository.save(existing)).willReturn(existing);

        commandService.update(messageId, new SlackMessageRequest.Update(
                "ROUTE_DAILY_SUMMARY",
                Map.of("data", Map.of("value", "테스트"))
        ));

        assertThat(existing.getTemplateCode()).isEqualTo("ROUTE_DAILY_SUMMARY");
        assertThat(existing.getPayload()).contains("테스트");
        verify(messageRepository).save(existing);
    }

    @Test
    @DisplayName("메시지를 삭제하면 저장소에서 제거된다")
    void delete_ShouldRemoveMessage() {
        UUID messageId = UUID.randomUUID();
        Message existing = Message.create(
                "SLACK123",
                "ROUTE_DAILY_SUMMARY",
                "{\"key\":\"value\"}",
                "본문"
        );
        given(messageRepository.findById(messageId)).willReturn(Optional.of(existing));

        commandService.delete(messageId);

        verify(messageRepository).delete(existing);
    }
}

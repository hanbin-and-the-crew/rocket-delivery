package org.sparta.slack.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta.common.event.slack.OrderDeadlineRequestedEvent;
import org.sparta.slack.application.port.out.SlackNotificationSender;
import org.sparta.slack.application.port.out.SlackRecipientFinder;
import org.sparta.slack.application.port.out.SlackTemplateRepository;
import org.sparta.slack.domain.entity.Message;
import org.sparta.slack.domain.entity.Template;
import org.sparta.slack.domain.enums.Channel;
import org.sparta.slack.domain.enums.TemplateFormat;
import org.sparta.slack.domain.repository.MessageRepository;
import org.sparta.slack.domain.enums.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.sparta.slack.support.fixture.OrderDeadlineEventFixture.payload;
import static org.sparta.slack.support.fixture.UserSlackViewFixture.approvedManager;

/** 주문 이벤트가 Slack 알림 흐름까지 이어지는지 확인하는 E2E 테스트. */
@SpringBootTest
@ActiveProfiles("test")
class OrderDeadlineFlowIntegrationTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockitoBean
    private SlackRecipientFinder slackRecipientFinder;
    @MockitoBean
    private SlackTemplateRepository slackTemplateRepository;
    @MockitoBean
    private MessageRepository messageRepository;
    @MockitoBean
    private SlackNotificationSender slackNotificationSender;

    @Test
    @DisplayName("주문 이벤트를 발행하면 Slack 알림 전송이 시도된다")
    void publishEvent_WithValidPayload_SendsSlackMessage() {
        // given
        Template template = Template.create(
                "ORDER_DEADLINE_ALERT",
                TemplateFormat.MARKDOWN,
                "최종 발송 시한은 {{finalDeadline}} 입니다.",
                Channel.SLACK,
                "주문 발송 시한 안내"
        );
        given(slackTemplateRepository.findActiveByCode("ORDER_DEADLINE_ALERT"))
                .willReturn(Optional.of(template));
        given(slackRecipientFinder.findApprovedByHubAndRoles(any(), any()))
                .willReturn(List.of(approvedManager(
                        UUID.randomUUID(),
                        HUB_ID,
                        UserRole.HUB_MANAGER,
                        "SLACK123"
                )));
        given(messageRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        OrderDeadlineRequestedEvent event = new OrderDeadlineRequestedEvent(
                UUID.randomUUID(),
                Instant.now(),
                payload(
                        UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"),
                        HUB_ID,
                        DESTINATION_ID,
                        Set.of(UserRole.HUB_MANAGER)
                )
        );

        // when
        transactionTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(event));

        // then
        verify(slackRecipientFinder, times(1))
                .findApprovedByHubAndRoles(eq(HUB_ID), eq(Set.of(UserRole.HUB_MANAGER)));
        verify(slackNotificationSender, times(1)).send(any(Message.class));
    }

    private static final UUID HUB_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID DESTINATION_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

}

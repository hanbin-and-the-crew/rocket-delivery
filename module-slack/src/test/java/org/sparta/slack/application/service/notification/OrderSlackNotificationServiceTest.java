package org.sparta.slack.application.service.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sparta.slack.application.dto.notification.OrderSlackMessagePayload;
import org.sparta.slack.application.dto.notification.OrderSlackNotificationRequest;
import org.sparta.slack.application.port.out.SlackNotificationSender;
import org.sparta.slack.application.port.out.SlackRecipientFinder;
import org.sparta.slack.application.port.out.SlackTemplateRepository;
import org.sparta.slack.domain.entity.Message;
import org.sparta.slack.domain.entity.Template;
import org.sparta.slack.domain.entity.UserSlackView;
import org.sparta.slack.domain.enums.Channel;
import org.sparta.slack.domain.enums.MessageStatus;
import org.sparta.slack.domain.enums.TemplateFormat;
import org.sparta.slack.domain.enums.UserRole;
import org.sparta.slack.domain.repository.MessageRepository;
import org.sparta.slack.support.fixture.UserSlackViewFixture;
import org.sparta.slack.support.fixture.ObjectMapperFixture;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 수신자 필터링과 전송 결과 처리를 검증하는 테스트.
 */
@ExtendWith(MockitoExtension.class)
class OrderSlackNotificationServiceTest {

    @Mock
    private SlackRecipientFinder recipientFinder;
    @Mock
    private SlackTemplateRepository templateRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private SlackNotificationSender notificationSender;

    private OrderSlackNotificationService orderSlackNotificationService;

    @BeforeEach
    void setUp() {
        orderSlackNotificationService = new OrderSlackNotificationService(
                recipientFinder,
                templateRepository,
                messageRepository,
                notificationSender,
                ObjectMapperFixture.defaultMapper()
        );
    }

    @Test
    @DisplayName("중복 Slack ID는 한 번만 전송한다")
    void notify_ShouldSendOncePerSlackId() {
        Template template = Template.create(
                "ORDER_DEADLINE_ALERT",
                TemplateFormat.MARKDOWN,
                "템플릿 {{orderNumber}}",
                Channel.SLACK,
                "desc"
        );
        when(templateRepository.findActiveByCode("ORDER_DEADLINE_ALERT"))
                .thenReturn(Optional.of(template));

        UUID hubId = UUID.randomUUID();
        UserSlackView view1 = UserSlackViewFixture.approvedManager(
                UUID.randomUUID(), hubId, UserRole.HUB_MANAGER, "SLACK123");
        UserSlackView view2 = UserSlackViewFixture.approvedManager(
                UUID.randomUUID(), hubId, UserRole.DELIVERY_MANAGER, "SLACK123");
        when(recipientFinder.findApprovedByHubAndRoles(eq(hubId), any()))
                .thenReturn(List.of(view1, view2));

        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderSlackNotificationService.notify(buildRequest(hubId));

        verify(notificationSender, times(1)).send(any(Message.class));
    }

    @Test
    @DisplayName("Slack 전송 실패 시 메시지를 실패 상태로 저장한다")
    void notify_WhenSendFails_ShouldMarkMessageFailed() {
        Template template = Template.create(
                "ORDER_DEADLINE_ALERT",
                TemplateFormat.MARKDOWN,
                "템플릿 {{orderNumber}}",
                Channel.SLACK,
                "desc"
        );
        when(templateRepository.findActiveByCode("ORDER_DEADLINE_ALERT"))
                .thenReturn(Optional.of(template));

        UUID hubId = UUID.randomUUID();
        UserSlackView view = UserSlackViewFixture.approvedManager(
                UUID.randomUUID(), hubId, UserRole.HUB_MANAGER, "SLACK123");
        when(recipientFinder.findApprovedByHubAndRoles(eq(hubId), any()))
                .thenReturn(List.of(view));

        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("failure")).when(notificationSender).send(any(Message.class));

        orderSlackNotificationService.notify(buildRequest(hubId));

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(Message::getStatus)
                .contains(MessageStatus.FAILED);
    }

    private OrderSlackNotificationRequest buildRequest(UUID hubId) {
        OrderSlackMessagePayload payload = new OrderSlackMessagePayload(
                UUID.randomUUID(),
                "ORDER-1",
                "홍길동",
                "hong@test.com",
                LocalDateTime.now(),
                "상품",
                "요청",
                "서울",
                "대전",
                "부산",
                "매니저",
                "manager@test.com",
                LocalDateTime.now().plusHours(4),
                "요약",
                "사유"
        );
        return new OrderSlackNotificationRequest(
                payload,
                hubId,
                Set.of(UserRole.HUB_MANAGER),
                "ORDER_DEADLINE_ALERT"
        );
    }
}

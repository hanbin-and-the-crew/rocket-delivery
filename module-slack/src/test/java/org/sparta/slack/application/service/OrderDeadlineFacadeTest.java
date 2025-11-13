package org.sparta.slack.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sparta.slack.application.notification.command.OrderDeadlineCommand;
import org.sparta.slack.application.notification.dto.OrderDeadlineNotificationResult;
import org.sparta.slack.application.notification.dto.OrderSlackNotificationRequest;
import org.sparta.slack.application.service.notification.OrderDeadlineFacade;
import org.sparta.slack.application.service.notification.OrderDeadlinePlanResult;
import org.sparta.slack.application.service.notification.OrderDeadlinePlanningService;
import org.sparta.slack.application.service.notification.OrderSlackNotificationService;
import org.sparta.slack.domain.enums.UserRole;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** OrderDeadlineFacade가 플래닝/알림을 연계하는지 검증한다. */
@ExtendWith(MockitoExtension.class)
class OrderDeadlineFacadeTest {

    @Mock
    private OrderDeadlinePlanningService planningService;
    @Mock
    private OrderSlackNotificationService notificationService;
    @InjectMocks
    private OrderDeadlineFacade orderDeadlineFacade;

    private OrderDeadlineCommand command;

    @BeforeEach
    void setUp() {
        command = new OrderDeadlineCommand(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                "ORD-1",
                "홍길동",
                "hong@test.com",
                LocalDateTime.of(2024, 7, 1, 10, 0),
                "상품",
                5,
                "긴급",
                "서울허브",
                "대전경유",
                "부산공항",
                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"),
                LocalDateTime.of(2024, 7, 2, 18, 0),
                9,
                18,
                Set.of(UserRole.HUB_MANAGER),
                "매니저",
                "manager@test.com"
        );
    }

    @Test
    @DisplayName("notify 메서드는 계획 결과를 Slack 알림으로 전달한다")
    void notify_WithValidPlan_SendsSlackNotification() {
        OrderDeadlinePlanResult planResult = new OrderDeadlinePlanResult(
                LocalDateTime.of(2024, 7, 2, 9, 30),
                "서울허브 → 부산공항",
                "AI 계산 사유",
                false
        );
        when(planningService.plan(command)).thenReturn(planResult);

        OrderDeadlineNotificationResult result = orderDeadlineFacade.notify(command);

        assertThat(result.orderId()).isEqualTo(command.orderId());
        assertThat(result.finalDeadline()).isEqualTo(planResult.finalDeadline());
        assertThat(result.aiReason()).isEqualTo(planResult.reason());
        assertThat(result.templateCode()).isEqualTo("ORDER_DEADLINE_ALERT");

        ArgumentCaptor<OrderSlackNotificationRequest> captor = ArgumentCaptor.forClass(OrderSlackNotificationRequest.class);
        verify(notificationService).notify(captor.capture());
        OrderSlackNotificationRequest request = captor.getValue();
        assertThat(request.templateCode()).isEqualTo("ORDER_DEADLINE_ALERT");
        assertThat(request.hubId()).isEqualTo(command.originHubId());
        assertThat(request.targetRoles()).containsExactlyInAnyOrderElementsOf(command.targetRoles());
    }
}

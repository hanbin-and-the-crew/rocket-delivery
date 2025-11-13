package org.sparta.slack.application.service.notification;

import lombok.RequiredArgsConstructor;
import org.sparta.slack.application.notification.command.OrderDeadlineCommand;
import org.sparta.slack.application.notification.dto.OrderDeadlineNotificationResult;
import org.sparta.slack.application.notification.dto.OrderSlackMessagePayload;
import org.sparta.slack.application.notification.dto.OrderSlackNotificationRequest;
import org.springframework.stereotype.Service;

/**
 * 발송 시한 계산부터 Slack 템플릿 전송까지를 조율하는 Facade.
 */
@Service
@RequiredArgsConstructor
public class OrderDeadlineFacade {

    static final String TEMPLATE_CODE = "ORDER_DEADLINE_ALERT";

    private final OrderDeadlinePlanningService planningService;
    private final OrderSlackNotificationService notificationService;

    public OrderDeadlineNotificationResult notify(OrderDeadlineCommand command) {
        OrderDeadlinePlanResult planResult = planningService.plan(command);

        OrderSlackMessagePayload payload = new OrderSlackMessagePayload(
                command.orderId(),
                command.orderNumber(),
                command.customerName(),
                command.customerEmail(),
                command.orderTime(),
                command.productInfo(),
                command.requestMemo(),
                command.origin(),
                command.transitPath(),
                command.destination(),
                command.deliveryManagerName(),
                command.deliveryManagerEmail(),
                planResult.finalDeadline(),
                planResult.routeSummary(),
                planResult.reason()
        );

        OrderSlackNotificationRequest request = new OrderSlackNotificationRequest(
                payload,
                command.originHubId(),
                command.targetRoles(),
                TEMPLATE_CODE
        );
        notificationService.notify(request);

        return new OrderDeadlineNotificationResult(
                command.orderId(),
                planResult.finalDeadline(),
                planResult.reason(),
                planResult.routeSummary(),
                TEMPLATE_CODE,
                command.targetRoles(),
                planResult.fallbackUsed()
        );
    }

}

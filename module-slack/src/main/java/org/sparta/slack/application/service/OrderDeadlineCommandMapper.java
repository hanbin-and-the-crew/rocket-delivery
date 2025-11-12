package org.sparta.slack.application.service;

import org.sparta.slack.application.command.OrderDeadlineCommand;
import org.sparta.slack.shared.event.OrderDeadlineRequestedEvent;

/**
 * 주문 이벤트 payload를 Slack 알림 Command로 변환하는 매퍼.
 */
public final class OrderDeadlineCommandMapper {

    private OrderDeadlineCommandMapper() {
    }

    public static OrderDeadlineCommand from(OrderDeadlineRequestedEvent.Payload payload) {
        if (payload == null) {
            throw new IllegalArgumentException("payload는 필수입니다");
        }

        return new OrderDeadlineCommand(
                payload.orderId(),
                payload.orderNumber(),
                payload.customerName(),
                payload.customerEmail(),
                payload.orderTime(),
                payload.productInfo(),
                payload.quantity(),
                payload.requestMemo(),
                payload.originLabel() != null ? payload.originLabel() : payload.originHubName(),
                payload.transitPath(),
                payload.destinationLabel() != null ? payload.destinationLabel() : payload.destinationCompanyName(),
                payload.originHubId(),
                payload.destinationCompanyId(),
                payload.deliveryDeadline(),
                payload.workStartHour(),
                payload.workEndHour(),
                payload.targetRoles(),
                payload.deliveryManagerName(),
                payload.deliveryManagerEmail()
        );
    }
}

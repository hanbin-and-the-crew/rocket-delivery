package org.sparta.slack.application.mapper;

import org.sparta.common.error.BusinessException;
import org.sparta.common.event.slack.OrderDeadlineRequestedEvent;
import org.sparta.slack.application.notification.command.OrderDeadlineCommand;
import org.sparta.slack.domain.enums.UserRole;
import org.sparta.slack.error.SlackErrorType;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * 주문 이벤트 payload를 Slack 알림 Command로 변환하는 매퍼.
 */
public final class OrderDeadlineCommandMapper {

    private OrderDeadlineCommandMapper() {
    }

    public static OrderDeadlineCommand from(OrderDeadlineRequestedEvent.Payload payload) {
        if (payload == null) {
            throw new BusinessException(SlackErrorType.SLACK_INVALID_ARGUMENT, "payload는 필수입니다");
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
                mapTargetRoles(payload.targetRoles()),
                payload.deliveryManagerName(),
                payload.deliveryManagerEmail()
        );
    }

    private static Set<UserRole> mapTargetRoles(Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return Collections.emptySet();
        }
        EnumSet<UserRole> roles = EnumSet.noneOf(UserRole.class);
        for (String name : roleNames) {
            if (name == null || name.isBlank()) {
                continue;
            }
            try {
                roles.add(UserRole.valueOf(name));
            } catch (IllegalArgumentException ex) {
                throw new BusinessException(SlackErrorType.SLACK_INVALID_ARGUMENT, "지원하지 않는 사용자 역할입니다: " + name);
            }
        }
        return roles;
    }
}

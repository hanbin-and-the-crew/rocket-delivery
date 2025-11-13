package org.sparta.slack.application.notification.command;

import org.sparta.common.error.BusinessException;
import org.sparta.slack.domain.enums.UserRole;
import org.sparta.slack.error.SlackErrorType;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 주문 발송 시한 계산 및 Slack 알림에 필요한 명령 DTO.
 */
public record OrderDeadlineCommand(
        UUID orderId,
        String orderNumber,
        String customerName,
        String customerEmail,
        LocalDateTime orderTime,
        String productInfo,
        Integer quantity,
        String requestMemo,
        String origin,
        String transitPath,
        String destination,
        UUID originHubId,
        UUID destinationHubId,
        LocalDateTime deliveryDeadline,
        Integer workStartHour,
        Integer workEndHour,
        Set<UserRole> targetRoles,
        String deliveryManagerName,
        String deliveryManagerEmail
) {

    public OrderDeadlineCommand {
        orderId = requireNonNull(orderId, "orderId는 필수입니다");
        orderNumber = requireNonNull(orderNumber, "orderNumber는 필수입니다");
        customerName = requireNonNull(customerName, "customerName은 필수입니다");
        orderTime = requireNonNull(orderTime, "orderTime은 필수입니다");
        productInfo = requireNonNull(productInfo, "productInfo는 필수입니다");
        quantity = requireNonNull(quantity, "quantity는 필수입니다");
        originHubId = requireNonNull(originHubId, "originHubId는 필수입니다");
        destinationHubId = requireNonNull(destinationHubId, "destinationHubId는 필수입니다");
        deliveryDeadline = requireNonNull(deliveryDeadline, "deliveryDeadline은 필수입니다");

        if (quantity <= 0) {
            throw new BusinessException(SlackErrorType.SLACK_INVALID_ARGUMENT, "quantity는 1 이상이어야 합니다");
        }

        workStartHour = defaultHour(workStartHour, 9);
        workEndHour = defaultHour(workEndHour, 18);

        if (workStartHour < 0 || workStartHour > 23) {
            throw new BusinessException(SlackErrorType.SLACK_INVALID_ARGUMENT, "workStartHour는 0~23 사이여야 합니다");
        }
        if (workEndHour < 0 || workEndHour > 23) {
            throw new BusinessException(SlackErrorType.SLACK_INVALID_ARGUMENT, "workEndHour는 0~23 사이여야 합니다");
        }
        if (workStartHour >= workEndHour) {
            throw new BusinessException(SlackErrorType.SLACK_INVALID_ARGUMENT, "workStartHour는 workEndHour보다 작아야 합니다");
        }

        targetRoles = (targetRoles == null || targetRoles.isEmpty())
                ? EnumSet.of(UserRole.HUB_MANAGER, UserRole.DELIVERY_MANAGER)
                : EnumSet.copyOf(targetRoles);
    }

    private static int defaultHour(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private static <T> T requireNonNull(T value, String message) {
        if (value == null) {
            throw new BusinessException(SlackErrorType.SLACK_INVALID_ARGUMENT, message);
        }
        return value;
    }

    /**
     * 경로 요약 기본값을 반환한다.
     */
    public String defaultRouteSummary() {
        StringBuilder builder = new StringBuilder();
        builder.append(Optional.ofNullable(origin).orElse("발송 허브"));

        if (transitPath != null && !transitPath.isBlank()) {
            builder.append(" → ").append(transitPath.trim());
        }
        if (destination != null && !destination.isBlank()) {
            builder.append(" → ").append(destination.trim());
        }
        return builder.toString();
    }
}

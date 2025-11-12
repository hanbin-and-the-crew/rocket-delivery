package org.sparta.slack.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 주문 기반 Slack 메시지에 사용할 페이로드 구조
 */
public record OrderSlackMessagePayload(
        UUID orderId,
        String orderNumber,
        String customerName,
        String customerEmail,
        LocalDateTime orderTime,
        String productInfo,
        String requestMemo,
        String origin,
        String transitPath,
        String destination,
        String deliveryManagerName,
        String deliveryManagerEmail,
        LocalDateTime finalDeadline,
        String routeSummary,
        String aiReason
) {
}

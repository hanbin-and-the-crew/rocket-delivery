package org.sparta.payment.application.command.refund;

import java.time.OffsetDateTime;

/**
 * PG 환불 웹훅을 도메인 계층에서 처리하기 위한 Command
 */
public record RefundPgWebhookCommand(
        String paymentKey,
        String refundKey,
        Long amount,
        String status,
        String failureCode,
        String failureMessage,
        OffsetDateTime occurredAt
) {
}
